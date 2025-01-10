INSERT INTO roles (id, created_at, updated_at, name)
    VALUES ('43b744ac-f3bd-47bb-a5fb-b83d0af760ab', '2024-08-30 21:51:32.425018', '2024-08-30 21:51:32.425035', 'ROLE_ADMIN');
INSERT INTO roles (id, created_at, updated_at, name)
    VALUES ('42999429-d402-4f32-947a-4cb99f2cf433', '2024-08-30 21:51:32.432524', '2024-08-30 21:51:32.432540', 'ROLE_USER');

INSERT INTO users (id, created_at, updated_at, email, last_name, name, password)
    VALUES ('a8d8ba08-7219-472a-87cd-386dc61195a5', '2024-08-30 21:51:32.441326', '2024-08-30 21:51:32.441332', 'admin@example.com', 'Administrator', 'Admin', '$2a$10$PktEiVtShSTWEgT/CqanyOGDm/l1hHK4BGyEPi0SIGZR1bj6SYl26');
INSERT INTO users (id, created_at, updated_at, email, last_name, name, password)
    VALUES ('3a409ad3-a1a2-4e64-9ad1-b33d28f153c3', '2024-08-30 21:51:32.444588', '2024-08-30 21:51:32.444602', 'user@example.com','DOE1', 'John1', '$2a$10$PktEiVtShSTWEgT/CqanyOGDm/l1hHK4BGyEPi0SIGZR1bj6SYl26');

INSERT INTO user_roles (user_id, role_id)
    VALUES ('a8d8ba08-7219-472a-87cd-386dc61195a5', '42999429-d402-4f32-947a-4cb99f2cf433');
INSERT INTO user_roles (user_id, role_id)
    VALUES ('a8d8ba08-7219-472a-87cd-386dc61195a5', '43b744ac-f3bd-47bb-a5fb-b83d0af760ab');
INSERT INTO user_roles (user_id, role_id)
    VALUES ('3a409ad3-a1a2-4e64-9ad1-b33d28f153c3', '42999429-d402-4f32-947a-4cb99f2cf433');
