package com.taobao.tdhs.config;


/**
 * 数据库类型枚举类型
 * 
 * @author qihao
 *
 */
public enum AtomDbTypeEnum {

	ORACLE(TAtomConstants.DEFAULT_ORACLE_DRIVER_CLASS, TAtomConstants.DEFAULT_ORACLE_SORTER_CLASS),

	MYSQL(TAtomConstants.DEFAULT_MYSQL_DRIVER_CLASS, TAtomConstants.DEFAULT_MYSQL_SORTER_CLASS);

	private String driverClass;
	private String sorterClass;

	AtomDbTypeEnum(String driverClass, String sorterClass) {
		this.driverClass = driverClass;
		this.sorterClass = sorterClass;
	}

	public static AtomDbTypeEnum getAtomDbTypeEnumByType(String type) {
		/*
		if (StringUtil.isNotBlank(type)) {
			for (AtomDbTypeEnum typeEnum : AtomDbTypeEnum.values()) {
				if (typeEnum.getType().equals(type.toUpperCase().trim())) {
					return typeEnum;
				}
			}
		}
		return null;
		*/
		try {
			return AtomDbTypeEnum.valueOf(type.trim().toUpperCase());
		} catch (Exception e) {
			return null;
		}
	}

	public String getDriverClass() {
		return driverClass;
	}

	public String getSorterClass() {
		return sorterClass;
	}

	/*public String getType() {
		return type;
	}*/
}
