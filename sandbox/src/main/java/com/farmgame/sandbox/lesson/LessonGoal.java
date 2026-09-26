package com.farmgame.sandbox.lesson;

/** Цель урока: проверяет состояние после выполнения программы игрока. */
@FunctionalInterface
public interface LessonGoal {

    GoalResult check(LessonContext context);

    /**
     * Цель выполнена, только если выполнены обе. При неудаче сообщение берётся из первой
     * невыполненной, при успехе — из первой (главной) цели.
     */
    default LessonGoal and(LessonGoal other) {
        return context -> {
            GoalResult first = check(context);
            if (!first.passed()) {
                return first;
            }
            GoalResult second = other.check(context);
            return second.passed() ? first : second;
        };
    }
}
