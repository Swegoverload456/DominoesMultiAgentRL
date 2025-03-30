import neat
import pickle
import socket

with open("best_network.pkl", "rb") as f:
    genome = pickle.load(f)
config = neat.Config(neat.DefaultGenome, neat.DefaultReproduction,
                    neat.DefaultSpeciesSet, neat.DefaultStagnation,
                    "config.txt")
net = neat.nn.FeedForwardNetwork.create(genome, config)

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(("localhost", 12345))
server.listen(1)

while True:
    conn, addr = server.accept()
    data = conn.recv(1024).decode().split(",")
    inputs = [float(x) for x in data]
    output = net.activate(inputs)
    conn.send(",".join(map(str, output)).encode())
    conn.close()