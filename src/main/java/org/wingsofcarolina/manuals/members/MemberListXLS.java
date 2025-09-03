package org.wingsofcarolina.manuals.members;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wingsofcarolina.manuals.domain.Member;

public class MemberListXLS extends MemberReader {

  public MemberListXLS(InputStream is) throws Exception {
    super(is);
  }

  public MemberListXLS(List<Member> all) {
    super(all);
  }

  @Override
  public List<String[]> readAllLines(InputStream is) throws IOException {
    boolean first = true;
    List<String[]> allLines = new ArrayList<String[]>();

    Document doc = Jsoup.parse(is, "UTF-8", "/");
    Elements rows = doc.select("tr");
    Elements columns;

    for (Element row : rows) {
      if (first) {
        columns = row.select("tr");
        first = false;
      } else {
        int index = 0;
        String[] elements = new String[5];
        columns = row.select("td");
        for (Element column : columns) {
          elements[index++] = column.text();
        }
        allLines.add(elements);
      }
    }
    return allLines;
  }
}
