package app.service;

import app.model.Rating;
import app.model.User;
import app.repo.RatingRepository;
import app.repo.UserRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LeaderboardService {
    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;

    public LeaderboardService(UserRepository userRepository, RatingRepository ratingRepository) {
        this.userRepository = userRepository;
        this.ratingRepository = ratingRepository;
    }

    public List<User> mostActiveUsers() {
        Map<Long, ActivityStats> activity = ratingRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Rating::getUserId,
                        r -> new ActivityStats(r.getActivityCount(), r.getUpdatedAt()),
                        (a, b) -> new ActivityStats(a.activityCount + b.activityCount,
                                a.lastActivity.isAfter(b.lastActivity) ? a.lastActivity : b.lastActivity)
                ));

        Comparator<User> comparator = Comparator
                .comparing((User u) -> activity.getOrDefault(u.getId(), ActivityStats.EMPTY).activityCount)
                .reversed()
                .thenComparing(u -> activity.getOrDefault(u.getId(), ActivityStats.EMPTY).lastActivity, Comparator.reverseOrder());

        return userRepository.findAllWithStats().stream()
                .filter(u -> activity.containsKey(u.getId()))
                .sorted(comparator)
                .toList();
    }

    private record ActivityStats(long activityCount, Instant lastActivity) {
        private static final ActivityStats EMPTY = new ActivityStats(0, Instant.EPOCH);
    }
}