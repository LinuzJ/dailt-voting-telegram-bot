FROM postgres

ENV POSTGRES_USER docker
ENV POSTGRES_DB polls

COPY init.sql /docker-entrypoint-initdb.d/
