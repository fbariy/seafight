create table invite(
    id uuid primary key,
    p1 varchar(255) not null,
    p2 varchar(255) not null,
    check (p1 != p2)
);