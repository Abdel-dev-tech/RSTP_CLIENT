JFLAGS = -g
JC = javac
JARFILE = RTSPClient.jar
SRC = $(shell find src -iname '*.java')
BIN = bin/production/RTSPClient
all: $(JARFILE)

.SUFFIXES: .java .class
$(BIN)/%.class: $(SRC)
	mkdir -p $(BIN)/
	$(JC) -sourcepath src -d $(BIN)/ $(JFLAGS) src/$*.java

$(JARFILE): $(BIN)/ca/yorku/rtsp/client/ui/MainWindow.class
	jar cvfe $(JARFILE) ca.yorku.rtsp.client.ui.MainWindow -C $(BIN) ca/

run: $(JARFILE)
	java -jar $(JARFILE)

clean:
	-rm -rf  $(JARFILE) $(BIN)/*
