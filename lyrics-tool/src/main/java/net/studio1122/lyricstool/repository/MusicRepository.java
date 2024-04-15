package net.studio1122.lyricstool.repository;

import net.studio1122.lyricstool.entity.Music;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MusicRepository extends MongoRepository<Music, String> {
}
