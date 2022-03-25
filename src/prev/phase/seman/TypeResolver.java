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

		public enum Mode {
			HEAD, BODY, CYCLE_CHECK
		}

		/**
		*  Return true if [tree] includes [type] in its definition tree
		*  unless this is the top most node.
		*/
		public boolean includesType(String type, SemType tree) {
			if (tree instanceof SemArr) {
				return includesType(type, ((SemArr) tree).elemType);
			} else if (tree instanceof SemBool || tree instanceof SemChar ||
					tree instanceof SemInt || tree instanceof SemVoid) {
				return false;
			} else if (tree instanceof SemName) {
				SemName semanticName = (SemName) tree;
				if (type.equals(semanticName.name)) {
					return true;
				} else {
					return includesType(type, semanticName.type());
				}
			} else if (tree instanceof SemPtr) {
				// Pointers in records break the cycle
				// return includesType(type, ((SemPtr) tree).baseType);
				return false;
			} else if (tree instanceof SemRec) {
				SemRec rec = (SemRec) tree;
				for (int i = 0; i < rec.numComps(); ++i) {
					if (includesType(type, rec.compType(i))) {
						return true;
					}
				}
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
			// System.out.println(nameType.name);
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
			System.out.println("testing");
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
				if (includesType(
						typeDecl.name,
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
