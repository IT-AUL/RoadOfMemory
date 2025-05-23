package com.itaul.rofm.model;

import com.itaul.rofm.model.enums.Language;
import com.itaul.rofm.model.enums.Type;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    @Column(columnDefinition = "char(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(length = 30)
    private String title;

    @Column(length = 150)
    private String promoUrl;

    @Column(length = 150)
    private String audioUrl;

    @Column(length = 200)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "quest_location",
            joinColumns = @JoinColumn(name = "quest_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    @ToString.Exclude
    private List<Location> locations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Type type;

    // Draft fields
    @Column(length = 30)
    private String titleDraft;

    @Column(length = 150)
    private String promoUrlDraft;

    @Column(length = 150)
    private String audioUrlDraft;

    @Column(length = 200)
    private String descriptionDraft;

    @ManyToMany
    @JoinTable(
            name = "quest_location_draft",
            joinColumns = @JoinColumn(name = "quest_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    @ToString.Exclude
    private List<Location> locationsDraft = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Language languageDraft;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Type typeDraft;

    @Column(nullable = false)
    private float rating = 0.0f;

    @Column(nullable = false)
    private int ratingCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @Column(nullable = false)
    private boolean published = false;

    public Quest(String title, Language language, Type type, User user) {
        this.titleDraft = title;
        this.languageDraft = language;
        this.user = user;
        this.typeDraft = type;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Quest quest = (Quest) o;
        return getId() != null && Objects.equals(getId(), quest.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
