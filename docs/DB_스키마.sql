create table users
(
    created_at datetime(6)                   null,
    deleted_at datetime(6)                   null,
    id         bigint auto_increment
        primary key,
    updated_at datetime(6)                   null,
    name       varchar(50)                   not null,
    role       enum ('CLASSMATE', 'CREATOR') not null
);

create table live_class
(
    capacity    int                              not null,
    end_date    date                             not null,
    price       decimal(12, 2)                   not null,
    start_date  date                             not null,
    created_at  datetime(6)                      null,
    creator_id  bigint                           not null,
    deleted_at  datetime(6)                      null,
    id          bigint auto_increment
        primary key,
    updated_at  datetime(6)                      null,
    title       varchar(200)                     not null,
    description text                             null,
    status      enum ('CLOSED', 'DRAFT', 'OPEN') not null,
    constraint FKkq3ijpymlywqm93e3af2cm64o
        foreign key (creator_id) references users (id)
);

create table enrollment
(
    cancelled_at datetime(6)                                null,
    class_id     bigint                                     not null,
    created_at   datetime(6)                                null,
    deleted_at   datetime(6)                                null,
    id           bigint auto_increment
        primary key,
    paid_at      datetime(6)                                null,
    updated_at   datetime(6)                                null,
    user_id      bigint                                     not null,
    status       enum ('CANCELLED', 'CONFIRMED', 'PENDING') not null,
    constraint FK4x08no2mpupkr616h50w3aksx
        foreign key (user_id) references users (id),
    constraint FKe29bscwm4wy5rjuc7xch2uod9
        foreign key (class_id) references live_class (id)
);

create table payment
(
    amount        decimal(12, 2)                                               not null,
    approved_at   datetime(6)                                                  null,
    created_at    datetime(6)                                                  null,
    deleted_at    datetime(6)                                                  null,
    enrollment_id bigint                                                       not null,
    id            bigint auto_increment
        primary key,
    updated_at    datetime(6)                                                  null,
    tid           varchar(100)                                                 not null,
    auth_token    varchar(200)                                                 null,
    failed_reason varchar(500)                                                 null,
    method        enum ('CARD')                                                not null,
    status        enum ('CANCELLED', 'FAILED', 'IN_PROGRESS', 'PAID', 'READY') not null,
    constraint UKee11ff7pnksu8khh5o8vxkm3t
        unique (tid),
    constraint FKn83c9242x6hpak4qj07f39jeu
        foreign key (enrollment_id) references enrollment (id)
);

create table waitlist_entry
(
    position       int         not null,
    active_user_id bigint      null,
    class_id       bigint      not null,
    created_at     datetime(6) null,
    deleted_at     datetime(6) null,
    id             bigint auto_increment
        primary key,
    updated_at     datetime(6) null,
    user_id        bigint      not null,
    constraint uk_waitlist_active_class_user
        unique (class_id, active_user_id),
    constraint FK7a1w2awuc2jabfu2gtbsog9h
        foreign key (user_id) references users (id),
    constraint FKiwv1go7wvgrfl1b9tspx5i55t
        foreign key (class_id) references live_class (id)
);

create index idx_waitlist_class
    on waitlist_entry (class_id, position);

