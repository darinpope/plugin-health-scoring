/*
 * MIT License
 *
 * Copyright (c) 2023-2025 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jenkins.pluginhealth.scoring.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.Score;
import io.jenkins.pluginhealth.scoring.repository.ScoreRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {
    private final ScoreRepository repository;

    public ScoreService(ScoreRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Score save(Score score) {
        return repository.save(score);
    }

    @Transactional(readOnly = true)
    public Optional<Score> latestScoreFor(Plugin plugin) {
        return repository.findFirstByPluginOrderByComputedAtDesc(plugin);
    }

    @Transactional(readOnly = true)
    public Map<String, Score> getLatestScoresSummaryMap() {
        return repository.findLatestScoreForAllPlugins().stream()
                .collect(Collectors.toMap(score -> score.getPlugin().getName(), score -> score));
    }

    @Transactional(readOnly = true)
    public Optional<ScoreStatistics> getScoresStatistics() {
        final int[] values = repository.getLatestScoreValueOfEveryPlugin();
        Arrays.sort(values);
        final int numberOfElement = values.length;

        return values.length == 0
                ? Optional.empty()
                : Optional.of(new ScoreStatistics(
                        Math.round((float) Arrays.stream(values).sum() / numberOfElement),
                        values[0],
                        values[numberOfElement - 1],
                        values[(int) (numberOfElement * .25)],
                        values[(int) (numberOfElement * .5)],
                        values[(int) (numberOfElement * .75)]));
    }

    @Transactional
    public int deleteOldScores() {
        return repository.deleteOldScoreFromPlugin();
    }

    public record ScoreStatistics(
            double average, int minimum, int maximum, int firstQuartile, int median, int thirdQuartile) {}

    @Transactional(readOnly = true)
    public Map<Integer, Long> getScoresDistribution() {
        final Map<Integer, Long> distribution = Arrays.stream(repository.getLatestScoreValueOfEveryPlugin())
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        for (int i = 0; i <= 100; i++) {
            distribution.merge(i, 0L, Long::sum);
        }
        return distribution;
    }

    @Transactional(readOnly = true)
    public List<Score> getAllLatestScoresWithValue(int value) {
        return repository.getAllLatestScoresWithValue(value);
    }
}
