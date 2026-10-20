#!/bin/sh
set -eu

# Render fornece a URL interna no formato postgresql://user:password@host/db.
# O driver JDBC recebe host/db; usuário e senha vêm de variáveis separadas.
if [ -n "${RENDER_POSTGRES_INTERNAL_URL:-}" ]; then
    case "$RENDER_POSTGRES_INTERNAL_URL" in
        postgresql://*@*)
            endereco="${RENDER_POSTGRES_INTERNAL_URL#postgresql://}"
            export BLOODELL_DB_URL="jdbc:postgresql://${endereco#*@}"
            ;;
        *)
            echo "RENDER_POSTGRES_INTERNAL_URL invalida" >&2
            exit 1
            ;;
    esac
fi

exec java -jar /app/bloodell.jar
