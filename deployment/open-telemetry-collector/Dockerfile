FROM otel/opentelemetry-collector-contrib

COPY otel-collector-config.yml /etc/otel-collector-config.yml

EXPOSE 4318
EXPOSE 13133

CMD ["--config=/etc/otel-collector-config.yml"]
