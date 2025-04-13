import styled from 'styled-components';
import Bank from './Bank';
import PlayerHand from './PlayerHand';
import React, { useRef } from 'react';
import { useDrop } from 'react-dnd';
import Tile, { ItemTypes } from './Tile';

const BoardArea = styled.div`
  display: flex;
  justify-content: center;
  height: 80vh;
  width: 80vw;
  background-color: rgb(247, 249, 231);
  position: relative;
  overflow: hidden;
  border-radius: 30px;
  &[data-is-over='true'] {
    background-color: rgb(11, 75, 84);
  }
`;

const BoardTilesContainer = styled.div`
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  gap: 10px;
  align-items: center;
  max-width: 90%;
  flex-wrap: wrap;
  justify-content: center;
  z-index: 1;
`;

const Board = ({ 
  bank, 
  hand,
  boardTiles,
  handleDropOnBank,
  handleDropOnHand,
  handleDropOnBoard,
  handleDragStart,
  speed 
}) => {
  const boardRef = useRef(null);

  const [{ isOver }, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item, monitor) => {
      if (!monitor.didDrop()) {
        handleDropOnBoard(item);
      }
    },
    collect: (monitor) => ({
      isOver: !!monitor.isOver(),
    }),
  }));

  return (
    <div id="boardDiv" ref={boardRef}>
      <BoardArea ref={drop} data-is-over={isOver}>
        <Bank 
          tiles={bank} 
          onDrop={handleDropOnBank}
          onDragStart={handleDragStart}
          speed={speed}
        />
        <PlayerHand 
          tiles={hand} 
          onDrop={handleDropOnHand}
          onDragStart={handleDragStart}
          speed={speed}
        />
        <BoardTilesContainer>
          {boardTiles.map((tile, index) => (
            <Tile
              key={`${tile.id}-${index}`}
              side1={tile.flipped ? tile.side2 : tile.side1}
              side2={tile.flipped ? tile.side1 : tile.side2}
              id={tile.id}
              dir="horizontal"
              flipped={tile.flipped}
              onDragStart={handleDragStart}
            />
          ))}
        </BoardTilesContainer>
      </BoardArea>
    </div>
  );
};

export default Board;