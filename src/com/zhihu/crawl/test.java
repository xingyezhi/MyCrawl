package com.zhihu.crawl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class test {

	
	public static Set<String> userset=Collections.synchronizedSet(new HashSet<String>());
	public static List<String> ulist=Collections.synchronizedList(new ArrayList<String>());
	public static void main(String []args){
		String name="abc";
		userset.add(name);
		String name2="abcd";
		System.out.println(userset.contains(name2));
	}
}
