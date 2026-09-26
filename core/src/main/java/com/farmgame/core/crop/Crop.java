package com.farmgame.core.crop;

import java.util.Objects;

/**
 * Описание культуры, которую игрок сажает на грядку.
 *
 * <p>Объект неизменяемый и создаётся только через {@link Builder} — это учебный пример
 * паттерна <b>Builder</b>:
 *
 * <pre>{@code
 * Crop corn = Crop.builder()
 *         .setType(CropType.CORN)
 *         .setName("Моя первая кукуруза")
 *         .setFertilized(true)
 *         .build();
 * }</pre>
 *
 * <p>Обязательное поле — только {@code type}; остальные имеют значения по умолчанию.
 */
public final class Crop {

    private final CropType type;
    private final String name;
    private final boolean fertilized;

    private Crop(Builder builder) {
        this.type = builder.type;
        this.name = builder.name != null ? builder.name : builder.type.displayName();
        this.fertilized = builder.fertilized;
    }

    /** Точка входа в Builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Короткая запись для простейшего случая: {@code Crop.of(CropType.WHEAT)}. */
    public static Crop of(CropType type) {
        return builder().setType(type).build();
    }

    public CropType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public boolean fertilized() {
        return fertilized;
    }

    /** Итоговый урожай с учётом удобрений. */
    public int expectedYield() {
        return fertilized ? type.baseYield() * 2 : type.baseYield();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Crop other
                && type == other.type
                && fertilized == other.fertilized
                && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, fertilized);
    }

    @Override
    public String toString() {
        return "Crop[" + type + ", name='" + name + "', fertilized=" + fertilized + "]";
    }

    /**
     * Пошаговый конструктор {@link Crop}. Методы возвращают {@code this},
     * поэтому вызовы можно объединять в цепочку.
     */
    public static final class Builder {

        private CropType type;
        private String name;
        private boolean fertilized;

        private Builder() {
        }

        /** Вид культуры (обязательно). */
        public Builder setType(CropType type) {
            this.type = type;
            return this;
        }

        /** Имя грядки для отображения (необязательно). */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /** Удобрена ли культура: удобрение удваивает урожай. */
        public Builder setFertilized(boolean fertilized) {
            this.fertilized = fertilized;
            return this;
        }

        /**
         * Проверяет заполненные поля и создаёт неизменяемый {@link Crop}.
         *
         * @throws IllegalStateException если не указан тип культуры
         */
        public Crop build() {
            if (type == null) {
                throw new IllegalStateException("Не указан тип культуры: вызовите setType(...) перед build()");
            }
            if (name != null && name.isBlank()) {
                throw new IllegalStateException("Имя культуры не может быть пустым");
            }
            return new Crop(this);
        }
    }
}
