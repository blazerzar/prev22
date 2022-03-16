package prev.data.typ;

import java.util.*;

import prev.common.logger.*;

/**
 * Record type.
 */
public class SemRec extends SemType {

	/** Component types. */
	private final SemType[] compTypes;

	public SemRec(Collection<SemType> compTypes) {
		this.compTypes = new SemType[compTypes.size()];
		int index = 0;
		for (SemType compType : compTypes) {
			this.compTypes[index++] = compType;
		}
	}

	/**
	 * Returns the type of the specified component.
	 * 
	 * @param idx The index of the component
	 * @return The type of the component.
	 */
	public SemType compType(int idx) {
		return compTypes[idx];
	}

	/**
	 * Returns the number of components.
	 * 
	 * @return The number of components.
	 */
	public int numComps() {
		return compTypes.length;
	}

	@Override
	public long size() {
		long size = 0;
		for (int index = 0; index < compTypes.length; index++) {
			size += compTypes[index].size();
		}
		return size;
	}

	@Override
	public void log(Logger logger) {
		if (logger == null)
			return;
		logger.begElement("semtype");
		logger.addAttribute("type", "RECORD");
		for (SemType compType : compTypes)
			compType.log(logger);
		logger.endElement();
	}

}
