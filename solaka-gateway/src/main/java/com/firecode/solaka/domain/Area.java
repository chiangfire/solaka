package com.firecode.solaka.domain;

import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import com.datastax.driver.core.DataType.Name;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 地区信息
 * @author JIANG
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table("k_area")
public class Area {
	
	@PrimaryKey
	@CassandraType(type=Name.BIGINT)
	private Long id;
	
	@CassandraType(type=Name.TEXT)
	private String name;
	/**
	 * 上级ID
	 */
	@Column("parent_id")
	@CassandraType(type=Name.BIGINT)
	@Indexed("parent_id")
	private Long parentId;
	/**
	 * 上级名称
	 */
	@Column("parent_name")
	@CassandraType(type=Name.TEXT)
	private String parentName;
	
	/**
	 * 区划代码
	 */
	@Column("area_code")
	@CassandraType(type=Name.TEXT)
	private String areaCode;
	
	/**
	 * 邮编
	 */
	@Column("zip_code")
	@CassandraType(type=Name.TEXT)
	private String zipCode;
	
	/**
	 * 用于查询子节点<p>避免递归</p>
	 */
	@Column("rule_id")
	@CassandraType(type=Name.TEXT)
	private String ruleId;
	
}
