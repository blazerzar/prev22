	package prev.phase.seman;

	import java.util.*;

	import prev.common.report.*;
	import prev.data.ast.tree.*;
	import prev.data.ast.tree.decl.*;
	import prev.data.ast.tree.expr.*;
	import prev.data.ast.tree.stmt.*;
	import prev.data.ast.tree.type.*;
	import prev.data.ast.visitor.*;
	import prev.data.typ.*;

	/**
	* Type resolver.
	*
	* Type resolver computes the values of {@link SemAn#declaresType},
	* {@link SemAn#isType}, and {@link SemAn#ofType}.
	*/
	public class TypeResolver extends AstFullVisitor<SemType, TypeResolver.Mode> {

		// List of arrays and records to check for void types
		List<AstArrType> arrTypes = new LinkedList<>();
		List<AstRecType> recTypes = new LinkedList<>();

		// To get component names for records
		Map<SemType, AstRecType> recCompNames = new HashMap<>();

		public enum Mode {
			HEAD, BODY, CYCLE_CHECK
		}

		/**
		*  Return true if [tree] includes any of [types] in its definition tree.
		*/
		public boolean includesType(Set<String> types, SemType tree) {
			if (tree instanceof SemArr) {
				return includesType(types, ((SemArr) tree).elemType);
			} else if (tree instanceof SemBool || tree instanceof SemChar ||
					tree instanceof SemInt || tree instanceof SemVoid) {
				return false;
			} else if (tree instanceof SemName) {
				SemName semanticName = (SemName) tree;
				if (types.contains(semanticName.name)) {
					return true;
				} else {
					types.add(semanticName.name);
					return includesType(types, semanticName.type());
				}
			} else if (tree instanceof SemPtr) {
				// Pointers in records break the cycle
				// return includesType(type, ((SemPtr) tree).baseType);
				return false;
			} else if (tree instanceof SemRec) {
				SemRec rec = (SemRec) tree;
				for (int i = 0; i < rec.numComps(); ++i) {
					if (includesType(types, rec.compType(i))) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Return true if both classes represent the same type. Use map [c] to
		 * prevent cycling.
		 */
		public boolean sameType(SemType a, SemType b, Map<String, Set<String>> c) {
			// Check for cycles when using aliases
			if (a instanceof SemName && b instanceof SemName) {
				SemName aa = (SemName) a;
				SemName bb = (SemName) b;

				// Check if we have visited [a] and [b] together already
				if (c.containsKey(aa.name)) {
					if (c.get(aa.name).contains(bb.name)) {
						return true;
					} else {
						c.get(aa.name).add(bb.name);
					}
				} else {
					c.put(aa.name, new HashSet<>());
					c.get(aa.name).add(bb.name);
				}
			}

			a = a.actualType();
			b = b.actualType();

			if (a instanceof SemArr && b instanceof SemArr) {
				SemArr aa = (SemArr) a;
				SemArr bb = (SemArr) b;
				return aa.numElems == bb.numElems &&
					sameType(aa.elemType, bb.elemType, c);
			} else if (a instanceof SemBool && b instanceof SemBool ||
					a instanceof SemChar && b instanceof SemChar ||
					a instanceof SemInt && b instanceof SemInt ||
					a instanceof SemVoid && b instanceof SemVoid) {
				return true;
			} else if (a instanceof SemPtr && b instanceof SemPtr) {
				SemPtr aa = (SemPtr) a;
				SemPtr bb = (SemPtr) b;
				return sameType(aa.baseType, bb.baseType, c);
			} else if (a instanceof SemRec && b instanceof SemRec) {
				SemRec aa = (SemRec) a;
				SemRec bb = (SemRec) b;
				if (aa.numComps() != bb.numComps()) return false;

				for (int i = 0; i < aa.numComps(); ++i) {
					if (!sameType(aa.compType(i), bb.compType(i), c)) {
						return false;
					}
				}

				return true;
			}
			return false;
		}

		// GENERAL PURPOSE

		@Override
		public SemType visit(AstTrees<? extends AstTree> trees, Mode mode) {
			// Add type declarations to the dictionary
			for (AstTree t : trees) {
				if (t instanceof AstTypeDecl) {
					t.accept(this, Mode.HEAD);
				}
			}

			// Resolve type declarations
			for (AstTree t : trees) {
				if (t instanceof AstTypeDecl) {
					t.accept(this, Mode.BODY);
				}
			}

			// Make sure array and record types do not include void type
			for (AstArrType arr : arrTypes) {
				SemArr type = (SemArr) SemAn.isType.get(arr);
				if (type.elemType.actualType() instanceof SemVoid) {
					throw new Report.Error(arr,
						"Void array type not allowed");
				}
			}

			for (AstRecType rec : recTypes) {
				SemRec type = (SemRec) SemAn.isType.get(rec);
				for (int i = 0; i < type.numComps(); ++i) {
					if (type.compType(i).actualType() instanceof SemVoid) {
						throw new Report.Error(rec.comps.get(i),
							"Void record component type not allowed");
					}
				}
			}

			// Check for cyclic name types
			for (AstTree t : trees) {
				if (t instanceof AstTypeDecl) {
					t.accept(this, Mode.CYCLE_CHECK);
				}
			}

			// Variables
			for (AstTree t : trees) {
				if (t instanceof AstVarDecl) {
					t.accept(this, null);
				}
			}

			// Check functions parameters and return types
			for (AstTree t : trees) {
				if (t instanceof AstFunDecl) {
					t.accept(this, Mode.HEAD);
				}
			}

			// Type check function body
			for (AstTree t : trees) {
				if (t instanceof AstFunDecl) {
					t.accept(this, Mode.BODY);
				}
			}

			return null;

		}

		// TYPES

		// T1
		@Override
		public SemType visit(AstAtomType atomType, Mode mode) {
			SemType type = switch (atomType.type) {
				case VOID -> new SemVoid();
				case CHAR -> new SemChar();
				case INT  -> new SemInt();
				case BOOL -> new SemBool();
			};
			SemAn.isType.put(atomType, type);
			return type;
		}

		// T2
		@Override
		public SemType visit(AstArrType arrType, Mode mode) {
			// Type check array element type
			SemType elemType = arrType.elemType.accept(this, mode);
			AstExpr num = arrType.numElems;

			// Size can be preffixed by a +
			if (num instanceof AstPfxExpr) {
				AstPfxExpr pfxExpr = (AstPfxExpr) num;
				if (pfxExpr.oper != AstPfxExpr.Oper.ADD) {
					throw new Report.Error(num,
						"Array size needs to be a positive integer constant");
				}

				num = pfxExpr.expr;
			}

			// Check if size is valid
			if (num instanceof AstAtomExpr) {
				AstAtomExpr numElemsExpr = (AstAtomExpr) num;

				if (numElemsExpr.type != AstAtomExpr.Type.INT) {
					throw new Report.Error(arrType.numElems,
						"Array size needs to be an integer constant");
				}

				try {
					long numElems = Long.parseLong(numElemsExpr.value);

					if (numElems <= 0) {
						throw new Report.Error(numElemsExpr,
							numElemsExpr.value + " : Array size needs to be positive");
					}

					SemType type = new SemArr(elemType, numElems);
					SemAn.isType.put(arrType, type);
					arrTypes.add(arrType);
					return type;
				} catch (Exception e) {
					throw new Report.Error(numElemsExpr,
						numElemsExpr.value + " : Illegal array size");
				}
			} else {
				throw new Report.Error(arrType.numElems,
					"Array size needs to be an integer constant");
			}
		}

		// T3
		@Override
		public SemType visit(AstRecType recType, Mode mode) {
			Set<String> declaredComps = new HashSet<>();

			// Type check record components
			List<SemType> compTypes = new LinkedList<>();
			for (AstCompDecl comp : recType.comps) {
				// Check if component with this name is already declared
				if (declaredComps.contains(comp.name)) {
					throw new Report.Error(comp,
						comp.name + " : Component with this name already declared");
				}

				declaredComps.add(comp.name);
				SemType type = comp.accept(this, mode);
				compTypes.add(type);
			}

			SemType type = new SemRec(compTypes);
			SemAn.isType.put(recType, type);
			recTypes.add(recType);
			recCompNames.put(type, recType);
			return type;
		}

		// T4
		@Override
		public SemType visit(AstPtrType ptrType, Mode mode) {
			// Type check base type
			SemType baseType = ptrType.baseType.accept(this, mode);
			SemType type = new SemPtr(baseType);
			SemAn.isType.put(ptrType, type);
			return type;
		}

		@Override
		public SemType visit(AstNameType nameType, Mode mode) {
			AstDecl typeDecl = SemAn.declaredAt.get(nameType);
			if (typeDecl instanceof AstTypeDecl) {
				SemType type = SemAn.declaresType.get((AstTypeDecl) typeDecl);
				SemAn.isType.put(nameType, type);
				// System.out.println("hello");
				return type;
			} else {
				throw new Report.Error(nameType,
					nameType.name + " : Type expected");
			}
		}

		// VALUE EXPRESSIONS

		@Override
		public SemType visit(AstNameExpr nameExpr, Mode mode) {
			AstDecl decl = SemAn.declaredAt.get(nameExpr);
			SemType type = null;

			// Variable access
			if (decl instanceof AstVarDecl) {
				AstVarDecl varDecl = (AstVarDecl) decl;
				type = SemAn.isType.get(varDecl.type);
			}

			// Parameter access
			if (decl instanceof AstParDecl) {
				AstParDecl parDecl = (AstParDecl) decl;
				type = SemAn.isType.get(parDecl.type);
			}

			// Parameterless function call
			if (decl instanceof AstFunDecl) {
				AstFunDecl funDecl = (AstFunDecl) decl;
				// Check if function actually takes no arguments
				if (funDecl.pars != null) {
					throw new Report.Error(nameExpr,
						nameExpr.name + " : Incorrect number of arguments provided");
				}
				type = SemAn.isType.get(funDecl.type);
			}


			if (type == null) {
				throw new Report.Error(nameExpr,
						nameExpr.name + " : Expression expected");
			}

			SemAn.ofType.put(nameExpr, type);
			return type;
		}

		// V1, V2
		@Override
		public SemType visit(AstAtomExpr atomExpr, Mode mode) {
			SemType type = switch (atomExpr.type) {
				case VOID    -> new SemVoid();
				case POINTER -> new SemPtr(new SemVoid());
				case STRING  -> new SemPtr(new SemChar());
				case BOOL    -> new SemBool();
				case CHAR    -> new SemChar();
				case INT     -> new SemInt();
			};
			SemAn.ofType.put(atomExpr, type);
			return type;
		}

		// V3, V8 (1 part), V9
		@Override
		public SemType visit(AstPfxExpr pfxExpr, Mode mode) {
			// Type check subexpression
			SemType expr = pfxExpr.expr.accept(this, mode);
			SemType type = null;

			// Bool negation (V3)
			if (pfxExpr.oper == AstPfxExpr.Oper.NOT) {
				if (expr.actualType() instanceof SemBool) {
					type = new SemBool();
				} else {
					throw new Report.Error(pfxExpr,
						"Bool expression expected");
				}
			}

			// +, - (V3)
			if (pfxExpr.oper == AstPfxExpr.Oper.ADD ||
					pfxExpr.oper == AstPfxExpr.Oper.SUB) {
				if (expr.actualType() instanceof SemInt) {
					type = new SemInt();
				} else {
					throw new Report.Error(pfxExpr,
						"Integer expression expected");
				}
			}

			// Pointer (V8)
			if (pfxExpr.oper == AstPfxExpr.Oper.PTR) {
				type = new SemPtr(expr);
			}

			// new, del (V9)
			if (pfxExpr.oper == AstPfxExpr.Oper.NEW) {
				if (expr.actualType() instanceof SemInt) {
					type = new SemPtr(new SemVoid());
				} else {
					throw new Report.Error(pfxExpr,
						"Integer expression expected");
				}
			}

			if (pfxExpr.oper == AstPfxExpr.Oper.DEL) {
				if (expr.actualType() instanceof SemPtr) {
					type = new SemVoid();
				} else {
					throw new Report.Error(pfxExpr,
						"Pointer expression expected");
				}
			}

			if (type != null) {
				SemAn.ofType.put(pfxExpr, type);
			}
			return type;
		}

		// V4, V5, V6, V7
		@Override
		public SemType visit(AstBinExpr binExpr, Mode mode) {
			SemType lhs = binExpr.fstExpr.accept(this, mode).actualType();
			SemType rhs = binExpr.sndExpr.accept(this, mode).actualType();

			SemType type = switch (binExpr.oper) {
				// V4
				case AND, OR -> {
					if (lhs instanceof SemBool && rhs instanceof SemBool) {
						yield new SemBool();
					}

					throw new Report.Error(binExpr,
						"Boolean operands required");
				}

				// V5
				case ADD, SUB, MUL, DIV, MOD -> {
					if (lhs instanceof SemInt && rhs instanceof SemInt) {
						yield new SemInt();
					}

					throw new Report.Error(binExpr,
						"Integer operands required");
				}

				// V6
				case EQU, NEQ -> {
					if (!sameType(lhs, rhs, new HashMap<>())) {
						throw new Report.Error(binExpr,
							"Cannot compare different types");
					}

					if (lhs instanceof SemBool && rhs instanceof SemBool ||
							lhs instanceof SemChar && rhs instanceof SemChar ||
							lhs instanceof SemInt && rhs instanceof SemInt ||
							lhs instanceof SemPtr && rhs instanceof SemPtr) {
						yield new SemBool();
					}

					throw new Report.Error(binExpr,
						"Can only compare boolean, character, integer and pointer operands");
				}

				// V7
				case LEQ, GEQ, LTH, GTH -> {
					if (!sameType(lhs, rhs, new HashMap<>())) {
						throw new Report.Error(binExpr,
							"Cannot compare different types");
					}

					if (lhs instanceof SemChar && rhs instanceof SemChar ||
							lhs instanceof SemInt && rhs instanceof SemInt ||
							lhs instanceof SemPtr && rhs instanceof SemPtr) {
						yield new SemBool();
					}

					throw new Report.Error(binExpr,
						"Can only relationally compare character, integer and pointer operands");
				}
			};

			SemAn.ofType.put(binExpr, type);
			return type;
		}

		// V8 (part 2)
		@Override
		public SemType visit(AstSfxExpr sfxExpr, Mode mode) {
			// Expression needs to be a pointer so we can dereference it
			SemType type = sfxExpr.expr.accept(this, mode).actualType();

			if (type instanceof SemPtr) {
				SemType baseType = ((SemPtr) type).baseType;
				SemAn.ofType.put(sfxExpr, baseType);
				return baseType;
			}

			throw new Report.Error(sfxExpr,
				"Can only dereference pointer expression");
		}

		// V10
		@Override
		public SemType visit(AstArrExpr arrExpr, Mode mode) {
			// First expression needs to be of array type
			SemType exprType = arrExpr.arr.accept(this, mode).actualType();
			if (!(exprType instanceof SemArr)) {
				throw new Report.Error(arrExpr.arr,
					"Cannot index a non array variable");
			}

			// Index must be of integer type
			SemType idxType = arrExpr.idx.accept(this, mode).actualType();
			if (!(idxType instanceof SemInt)) {
				throw new Report.Error(arrExpr.idx,
					"Array index needs to be an integer");
			}

			SemArr arrType = (SemArr) exprType;
			SemAn.ofType.put(arrExpr, arrType.elemType);
			return arrType.elemType;
		}

		// V11
		@Override
		public SemType visit(AstRecExpr recExpr, Mode mode) {
			// Get name type
			SemType nameType = recExpr.rec.accept(this, mode).actualType();
			AstNameExpr recName = (AstNameExpr) recExpr.rec;
			AstDecl decl = SemAn.declaredAt.get(recName);

			if (nameType instanceof SemRec && decl instanceof AstVarDecl) {
				AstVarDecl recDecl = (AstVarDecl) decl;
				AstRecType recCompType = recCompNames.get(nameType);

				// Check if component name is valid
				int idx = 0;
				for (AstCompDecl comp : recCompType.comps) {
					if (comp.name.equals(recExpr.comp.name)) {
						SemType exprType = ((SemRec) nameType).compType(idx);
						SemAn.ofType.put(recExpr, exprType);
						return exprType;
					}

					++idx;
				}

				// Component name is not found
				throw new Report.Error(recExpr,
					recExpr.comp.name + " : Unknown record component name");
			}

			throw new Report.Error(recExpr,
				recName.name + " : Record expected");
		}

		// V12
		@Override
		public SemType visit(AstCallExpr callExpr, Mode mode) {
			// Make sure call is done on a function
			AstDecl decl = SemAn.declaredAt.get(callExpr);
			if (!(decl instanceof AstFunDecl)) {
				throw new Report.Error(callExpr,
					callExpr.name + " : Only functions are callable");
			}

			AstFunDecl funDecl = (AstFunDecl) decl;

			// Type check arguments
			if (funDecl.pars.size() != callExpr.args.size()) {
				throw new Report.Error(callExpr,
					callExpr.name + " : Incorrect number of arguments provided");
			}

			for (int i = 0 ; i < callExpr.args.size(); ++i) {
				SemType argType = callExpr.args.get(i).accept(this, mode).actualType();
				if (argType instanceof SemBool || argType instanceof SemChar ||
						argType instanceof SemInt || argType instanceof SemPtr) {
					SemType parType = funDecl.pars.get(i).accept(this, mode).actualType();
					if (!sameType(parType, argType, new HashMap<>())) {
						throw new Report.Error(callExpr.args.get(i),
							"Incorrect type of argument provided");
					}
				} else {
					throw new Report.Error(callExpr.args.get(i),
						"Illegal argument provided");
				}
			}

			SemType retType = SemAn.isType.get(funDecl.type);
			SemAn.ofType.put(callExpr, retType);
			return retType;
		}

		// V13
		@Override
		public SemType visit(AstStmtExpr stmtExpr, Mode mode) {
			// Type check every statement and remember the last type
			SemType type = null;
			for (AstStmt stmt : stmtExpr.stmts) {
				type = stmt.accept(this, mode);
			}

			SemAn.ofType.put(stmtExpr, type);
			return type;
		}

		// V14
		@Override
		public SemType visit(AstCastExpr castExpr, Mode mode) {
			SemType expr = castExpr.expr.accept(this, mode).actualType();
			SemType type = castExpr.type.accept(this, mode);
			SemType cast = type.actualType();

			// Can only cast from char, int and ptr
			if (!(expr instanceof SemChar || expr instanceof SemInt ||
					expr instanceof SemPtr)) {
				throw new Report.Error(castExpr.expr,
					"Only casts from char, int and ptr are legal");
			}

			// Can only cast to char, int and ptr
			if (!(cast instanceof SemChar || cast instanceof SemInt ||
					cast instanceof SemPtr)) {
				throw new Report.Error(castExpr.type,
					"Only casts to char, int and ptr are legal");
			}

			SemAn.ofType.put(castExpr, type);
			return type;
		}

		// V15
		@Override
		public SemType visit(AstWhereExpr whereExpr, Mode mode) {
			SemType decls = whereExpr.decls.accept(this, mode);
			SemType type = whereExpr.expr.accept(this, mode);
			SemAn.ofType.put(whereExpr, type);
			return type;
		}

		// STATEMENTS

		// S1
		@Override
		public SemType visit(AstAssignStmt assignStmt, Mode mode) {
			// Make sure both sides are of the same type
			SemType lhs = assignStmt.dst.accept(this, mode).actualType();
			SemType rhs = assignStmt.src.accept(this, mode).actualType();
			if (!sameType(lhs, rhs, new HashMap<>())) {
				throw new Report.Error(assignStmt,
					"Cannot assign to a different type");
			}

			// Only certain types are assignable
			if (!(lhs instanceof SemBool || lhs instanceof SemChar ||
					lhs instanceof SemInt || lhs instanceof SemPtr)) {
				throw new Report.Error(assignStmt,
					"Illegal type for assignment");
			}

			SemType type = new SemVoid();
			SemAn.ofType.put(assignStmt, type);
			return type;
		}

		@Override
		public SemType visit(AstExprStmt exprStmt, Mode mode) {
			SemType type = exprStmt.expr.accept(this, mode);
			SemAn.ofType.put(exprStmt, type);
			return type;
		}

		// S2
		@Override
		public SemType visit(AstIfStmt ifStmt, Mode mode) {
			SemType cond = ifStmt.cond.accept(this, mode).actualType();
			SemType thenType = ifStmt.thenStmt.accept(this, mode);
			SemType elseType = ifStmt.elseStmt.accept(this, mode);

			// Condition needs to be bool
			if (!(cond instanceof SemBool)) {
				throw new Report.Error(ifStmt,
					"Condition in if statements needs to be of boolean type");
			}

			SemType type = new SemVoid();
			SemAn.ofType.put(ifStmt, type);
			return type;
		}

		// S3
		@Override
		public SemType visit(AstWhileStmt whileStmt, Mode mode) {
			SemType cond = whileStmt.cond.accept(this, mode).actualType();
			// Condition needs to be bool
			if (!(cond instanceof SemBool)) {
				throw new Report.Error(whileStmt,
					"Condition in while statements needs to be of boolean type");
			}

			SemType body = whileStmt.bodyStmt.accept(this, mode);

			SemType type = new SemVoid();
			SemAn.ofType.put(whileStmt, type);
			return type;

		}

		// DECLARATIONS

		@Override
		public SemType visit(AstCompDecl compDecl, Mode mode) {
			// Type check component type
			return compDecl.type.accept(this, mode);
		}

		// D1
		@Override
		public SemType visit(AstTypeDecl typeDecl, Mode mode) {
			if (mode == Mode.HEAD) {
				// Save declared type name
				SemName type = new SemName(typeDecl.name);
				SemAn.declaresType.put(typeDecl, type);
				return type;
			} else if (mode == Mode.BODY) {
				// Type check variable type
				SemName type = SemAn.declaresType.get(typeDecl);
				type.define(typeDecl.type.accept(this, mode));
				return type;
			} else if (mode == Mode.CYCLE_CHECK) {
				Set<String> types = new HashSet<>();
				types.add(typeDecl.name);
				if (includesType(
						types,
						SemAn.declaresType.get(typeDecl).type())) {
					throw new Report.Error(typeDecl,
						typeDecl.name + " : Cyclic type not allowed");
				}
				return null;
			}
			return null;
		}

		// D2
		@Override
		public SemType visit(AstVarDecl varDecl, Mode mode) {
			// Get type and make sure it is not void
			SemType type = varDecl.type.accept(this, mode);
			if (type.actualType() instanceof SemVoid) {
				throw new Report.Error(varDecl,
					varDecl.name + " : Void variable type not allowed");
			}

			return null;
		}

		// D3, D4

		@Override
		public SemType visit(AstFunDecl funDecl, Mode mode) {
			if (mode == Mode.HEAD) {
				// Type check parameters if present
				if (funDecl.pars != null) {
					for (AstParDecl par : funDecl.pars) {
						par.accept(this, mode);
					}
				}

				// Type check return type
				SemType type = funDecl.type.accept(this, mode).actualType();
				// Allowed return types are void, bool, char, int and ptr
				if (type instanceof SemVoid || type instanceof SemBool ||
						type instanceof SemChar || type instanceof SemInt ||
						type instanceof SemPtr) {
					return null;
				}

				throw new Report.Error(funDecl,
					funDecl.name + " : Illegal function return type");
			} else if (mode == Mode.BODY) {
				// Type check expression if present
				if (funDecl.expr != null) {
					SemType retType = SemAn.isType.get(funDecl.type).actualType();
					SemType bodyType = funDecl.expr.accept(this, mode).actualType();
					// Expression type need to match return type
					if (sameType(retType, bodyType, new HashMap<>())) {
						return null;
					}

					throw new Report.Error(funDecl,
						funDecl.name + " : Function return and expression type do not match");

				}
			}

			return null;
		}

		@Override
		public SemType visit(AstParDecl parDecl, Mode mode) {
			// Allowed types are bool, char, int and ptr
			SemType type = parDecl.type.accept(this, mode).actualType();
			if (type instanceof SemBool || type instanceof SemChar ||
					type instanceof SemInt || type instanceof SemPtr) {
				return type;
			}

			throw new Report.Error(parDecl,
				parDecl.name + " : Illegal parameter type");
		}

	}
