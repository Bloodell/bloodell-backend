-- Impede identidades ambiguas no login, sem reescrever e-mails ou hashes existentes.
-- Se houver duplicatas legadas, a migration falha: revisar as contas antes de ativar.
CREATE UNIQUE INDEX ux_usuarios_email_normalizado ON usuarios (lower(btrim(email)));
