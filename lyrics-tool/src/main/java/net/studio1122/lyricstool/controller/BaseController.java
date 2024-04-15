package net.studio1122.lyricstool.controller;

import lombok.extern.slf4j.Slf4j;
import net.studio1122.lyricstool.entity.Music;
import net.studio1122.lyricstool.service.MusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Slf4j
public class BaseController {

    private MusicService musicService;

    @Autowired
    public BaseController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/form")
    public String inputForm() {
        return "form";
    }

    @PostMapping("/api/music")
    public String save(@ModelAttribute Music music) {
        musicService.save(music);
        //log.debug("music={}", music);
        return "redirect:/form";
    }
}
