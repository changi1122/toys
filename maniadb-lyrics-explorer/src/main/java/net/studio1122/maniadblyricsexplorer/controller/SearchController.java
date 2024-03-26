package net.studio1122.maniadblyricsexplorer.controller;

import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import net.studio1122.maniadblyricsexplorer.service.APIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    APIService apiService;

    @Autowired
    public SearchController(APIService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/search")
    public String albumSearch(Model model, @RequestParam(value = "query", defaultValue = "") String query) throws Exception {
        List<SearchResultItem> items = apiService.albumSearch(query);

        model.addAttribute("query", query);
        model.addAttribute("items", items);

        return "search";
    }
}
