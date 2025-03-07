class Tile:
    def __init__(self, a, b):
        self.side1 = a
        self.side2 = b

    def getA(self):
        return self.side1

    def getB(self):
        return self.side2

    def sum(self):
        return self.side1 + self.side2

    def __str__(self):
        return f"{self.side1}:{self.side2}"

    def __repr__(self):
        return self.__str__()

    def __eq__(self, other):
        return (self.side1 == other.side1 and self.side2 == other.side2) or (
            self.side1 == other.side2 and self.side2 == other.side1
        )

    def __lt__(self, other):
        if self.side2 == other.side2:
            return self.side1 < other.side1
        return self.side2 < other.side2