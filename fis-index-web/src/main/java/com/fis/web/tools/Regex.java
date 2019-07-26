package com.fis.web.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Regex {
	/**
	 * 正则匹配字符之间内容
	 */
	public static String getRegexStr(String str, String strtpart,
			String endpart, int position) {
		Pattern p = Pattern.compile("(?<=" + strtpart + ")[\\S\\s]+?(?="
				+ endpart + ")");
		Matcher result = p.matcher(str);

		// if(position<result.end())
		// return result.group(0);
		int i = 1;
		while (result.find()) {
			if (i == position)
				return result.group();

			i++;
		}

		return "";
	}

	/**
	 * 正则匹配，返回所有结果
	 * */
	public static List<String> getRegexStrAll(String str, String strtpart,
			String endpart) {
		Pattern p = Pattern.compile("(?<=" + strtpart + ").*?(?=" + endpart
				+ ")");
		Matcher result = p.matcher(str);
		List<String> m = new ArrayList<String>();

		int i = 0;
		while (result.find()) {
			m.add(result.group());

			i++;
		}

		return m;
	}
	/**
	 * 过滤所有html代码
	 * 
	 * @param htmlStr
	 * @return
	 */
	public static String filterHtml(String htmlStr) {
		try {
			if (!StringUtils.hasText(htmlStr))
				return "";
			htmlStr = htmlStr.replace("＜", "<");
			htmlStr = htmlStr.replace("＞", ">");
			htmlStr = htmlStr.replace("\r\n", "");

			String regEx_html = "<[^>]+>";
			Pattern p_html = Pattern.compile(regEx_html);
			Matcher m_html = p_html.matcher(htmlStr);
			htmlStr = m_html.replaceAll(""); // 过滤html标签

			regEx_html = "http://.*\\.(jpg|html|aspx|htm|jsp|gif)";
			p_html = Pattern.compile(regEx_html);
			m_html = p_html.matcher(htmlStr);
			htmlStr = m_html.replaceAll(""); // 过滤html标签

		} catch (Exception ex) {
		}
		return htmlStr;
	}
}
