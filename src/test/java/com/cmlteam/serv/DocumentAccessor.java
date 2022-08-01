package com.cmlteam.serv;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DocumentAccessor {
    private final Document document;

    int getFileCount() {
        Elements trElements = document.select("table tbody tr");
        return trElements.size();

    }
    List<String> getFileNames() {
        Elements trElements = document.select("table tbody tr");
        return trElements.stream().map(element -> element.select("td").first().text()).collect(Collectors.toList());
    }

    Map<String,String> getFolderLinks() {
        Map<String,String> res = new HashMap<>();
        Elements trElements = document.select("table tbody tr");
        for (Element trElement : trElements) {
            Element td = trElement.select("td").first();
            Element a = td.select("a").first();
            if (a !=null) {
                res.put(td.text(), a.attr("href"));
            }
        }
        return res;
    }
}
