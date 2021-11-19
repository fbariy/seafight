create table game(
  id uuid primary key,
  invite_id uuid not null unique,
  p1_ships json not null,
  p2_ships json not null,
  turns json not null,
  foreign key (invite_id) references invite(id)
);
