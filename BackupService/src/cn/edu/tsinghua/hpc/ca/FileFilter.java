package cn.edu.tsinghua.hpc.ca;

import java.io.File;

/**
 * <p>
 * Title: CheckUserForCPK
 * </p>
 *
 * <p>
 * Description: �ļ���������ʵ���˴�ָ��Ŀ¼�а��չؼ��ֲ����ļ��Ĺ��ܣ��ؼ���λ���ļ����Ŀ�ͷ���β��
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

	public FileFilter(String keyword) {// ���캯��
		this.keyword = keyword;
	}

	@Override
	public boolean accept(File pathname) {
		String fileName = pathname.getName().toLowerCase();

		if (fileName.endsWith("_cpk.pem") && fileName.startsWith(keyword)) {// ���ù��˹���
			return true;
		} else {
			return false;
		}
	}

	/**
	 * ���Է���
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
