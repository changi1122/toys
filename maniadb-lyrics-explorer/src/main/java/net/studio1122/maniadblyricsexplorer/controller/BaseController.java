package net.studio1122.maniadblyricsexplorer.controller;

import net.studio1122.maniadblyricsexplorer.entity.Album;
import net.studio1122.maniadblyricsexplorer.entity.SearchResultItem;
import net.studio1122.maniadblyricsexplorer.entity.Song;
import net.studio1122.maniadblyricsexplorer.service.APIService;
import net.studio1122.maniadblyricsexplorer.utility.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BaseController {

    APIService apiService;

    @Autowired
    public BaseController(APIService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/search")
    public String albumSearch(Model model, @RequestParam(value = "query", defaultValue = "") String query) throws Exception {
        List<SearchResultItem> items = apiService.albumSearch(query);

        model.addAttribute("query", query);
        model.addAttribute("items", items);

        return "search";
    }

    @GetMapping("/album/{id}")
    public String album(Model model, @PathVariable("id") String id) throws Exception {
        Pair<Album, List<Song>> albumAndSongs = apiService.album(id);

        model.addAttribute("album", albumAndSongs.getFirst());
        model.addAttribute("songs", albumAndSongs.getSecond());

        return "album";
    }
}
