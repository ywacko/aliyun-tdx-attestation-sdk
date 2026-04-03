CC ?= gcc
CFLAGS ?= -O2 -Wall -Wextra
LDFLAGS ?=
LDLIBS ?= -ltdx_attest

TARGET := tdx-quote-helper
SRC := tdx-quote-helper.c

.PHONY: all clean

all: $(TARGET)

$(TARGET): $(SRC)
	$(CC) $(CFLAGS) -o $@ $< $(LDFLAGS) $(LDLIBS)

clean:
	rm -f $(TARGET)
