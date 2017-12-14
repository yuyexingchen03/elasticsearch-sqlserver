package com.hwapu.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3p0Utils {
	private static final Logger LOGGER = Logger.getLogger(C3p0Utils.class);  
	// 通过标识名来创建相应连接池
	//private static ComboPooledDataSource dataSource = new ComboPooledDataSource("mysql");
	private static ComboPooledDataSource dataSource;
	static {
		//WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
		dataSource = new ComboPooledDataSource("mysql");
	}
	// 从连接池中取用一个连接
	public static Connection getConnection() {
		try {
			return dataSource.getConnection();

		} catch (Exception e) {
			LOGGER.error("Exception in C3p0Utils!", e);
			e.printStackTrace();
			throw new RuntimeException("Exception in C3p0Utils!");
		}
		
	}

	// 释放连接回连接池
	public static void close(Connection conn, PreparedStatement pst, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				LOGGER.error("Exception in C3p0Utils!", e);
				e.printStackTrace();
			}
		}
		if (pst != null) {
			try {
				pst.close();
			} catch (SQLException e) {
				LOGGER.error("Exception in C3p0Utils!", e);
				e.printStackTrace();
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				LOGGER.error("Exception in C3p0Utils!", e);
				e.printStackTrace();
			}
		}
	}

	public static void close(PreparedStatement ps, ResultSet rs) {
		if (rs != null) {
			try {

				rs.close();

			} catch (Exception e) {
				rs = null;
				LOGGER.error("Exception in C3p0Utils!", e);
				e.printStackTrace();
			}
		}
		if (ps != null) {
			try {

				ps.close();

			} catch (Exception e) {
				ps = null;
				LOGGER.error("Exception in C3p0Utils!", e);
				e.printStackTrace();
			}
		}
		
	}
}