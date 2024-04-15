package net.studio1122.lyricstool.service;

import net.studio1122.lyricstool.entity.Music;
import net.studio1122.lyricstool.repository.MusicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

@Service
public class MusicService {

    private MusicRepository repository;

    @Autowired
    public MusicService(MusicRepository repository) {
        this.repository = repository;
    }

    public void save(Music music) {
        if (music.getTitle().isEmpty() || music.getArtist().isEmpty() || music.getRelease().isEmpty() ||
                music.getNation().isEmpty() || music.getGenre().isEmpty() || music.getLyric().isEmpty()) {
            return;
        }

        repository.save(music);
    }
}
