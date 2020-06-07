package com.github.chrisgleissner.sandbox.neo4j.multipledbs;

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Migrator {

    public static <S, T> int migrate(Stream<S> sourceStream,
                                     CrudRepository<T, ?> targetRepo,
                                     Function<S, T> mapper,
                                     int chunkSize) {
        log.info("Started data migration");
        long startTime = System.nanoTime();
        AtomicInteger migrationCount = new AtomicInteger(0);
        Iterables.partition(sourceStream::iterator, chunkSize).forEach(chunk -> {
            List<T> mappedChunk = chunk.stream().map(mapper).collect(Collectors.toList());
            targetRepo.saveAll(mappedChunk);
            migrationCount.addAndGet(mappedChunk.size());
            log.info("Migrated {} entities", migrationCount.get());
        });
        log.info("Migrated {} in {}ms", migrationCount.get(), (System.nanoTime() - startTime) / 1_000_000);
        return migrationCount.get();
    }
}
