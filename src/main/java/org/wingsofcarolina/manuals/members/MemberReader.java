package org.wingsofcarolina.manuals.members;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.domain.Member;

public abstract class MemberReader {
	private static final Logger logger = LoggerFactory.getLogger(MemberReader.class);

	Map<Integer, Member> memberList = new HashMap<Integer, Member>();
	
	public MemberReader() {}
	
	public MemberReader(InputStream is) throws Exception {
		List<String[]> list;
		
		list = readAllLines(is);
		Iterator<String[]> it = list.iterator();
		while (it.hasNext()) {
			String[] row = it.next();
			Integer id = Integer.parseInt(row[0]);
			String name = row[1] + " " + row[2];
			String email = row[3];
			Integer level = Integer.parseInt(row[4]);
			Member member = new Member(id, name, email, level);
			
			addMember(member);
		}
	}
	
	public MemberReader(List<Member> all) {
		Iterator<Member> it = all.iterator();
		while (it.hasNext()) {
			Member member = it.next();
			memberList.put(member.getId(), member);
		}
	}

	abstract public List<String[]> readAllLines(InputStream is) throws Exception;
	
	public Map<Integer, Member> members() {
		return memberList;
	}
	
	public void addMember(Member member) {
		memberList.put(member.getId(), member);
	}
	
	public void remove(Member target) {
	    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<Integer, Member> entry = iterator.next();
	        //Integer id = entry.getKey();
	        Member member = entry.getValue();
	        
	        if (target.getEmail().compareTo(member.getEmail()) == 0) {
	        	iterator.remove();
	        }
	    }
	}

	public boolean hasMember(Member member) {
		if (memberList.containsKey(member.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public void clean() {
	    Iterator<Map.Entry<Integer, Member>> iterator = memberList.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<Integer, Member> entry = iterator.next();
	        Member member = entry.getValue();
	        if (isNotActive(member)|| isCruft(member)) {
	        	iterator.remove();
	        }
	    }
	}
	
	private boolean isNotActive(Member member) {
		Integer level = member.getLevel();
		switch (level) {
			case 2 : return true;
			case 6 : return true;
			case 7 : return true;
			default: return false;
		}
	}
	
	private boolean isCruft(Member member) {
		String name = member.getName();
		switch (name) {
			case "Childrens Flight Hope" : return true;
			case "Joe Pilot" : return true;
			case "Jane Pilot" : return true;
			case "Maintenance Maintenance" : return true;
			case "Book Keeper" : return true;
			case "Club Trips" : return true;
		}
		return false;
	}
}
