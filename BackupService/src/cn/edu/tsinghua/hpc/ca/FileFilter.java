package cn.edu.tsinghua.hpc.ca;

import java.io.File;

/**
 * <p>
 * Title: CheckUserForCPK
 * </p>
 *
 * <p>
 * Description: 文件过滤器（实现了从指定目录中按照关键字查找文件的功能：关键字位于文件名的开头或结尾）
 * </p>
 *
 * <p>
 * Copyright: Copyright (c) 2011
 * </p>
 *
 * <p>
 * Company: ccit
 * </p>
 *
 * @author chensonglin
 * @date 2011.5.20
 * @version 1.0
 */
class FileFilter implements java.io.FileFilter {

	private String keyword;

	public FileFilter(String keyword) {// 构造函数
		this.keyword = keyword;
	}

	@Override
	public boolean accept(File pathname) {
		String fileName = pathname.getName().toLowerCase();

		if (fileName.endsWith("_cpk.pem") && fileName.startsWith(keyword)) {// 设置过滤规则
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 测试方法
	 */
	public static void main(String[] args) {
		File path = new File("d:/");
		String imsi = "310260000000000";
		File[] list = path.listFiles(new FileFilter(imsi));
		for (int i = 0; i < list.length; i++) {
			System.out.println(list[i].getName());
		}
	}

}
