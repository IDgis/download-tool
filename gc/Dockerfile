FROM ubuntu:xenial

RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		cron \
	&& rm -rf /var/lib/apt/lists/*
	
COPY start.sh /opt/
COPY gc.sh /opt/
	
RUN chmod u+x /opt/*.sh

CMD ["/opt/start.sh"]