import React from 'react';
import { useDrop } from 'react-dnd';
import styled from 'styled-components';
import Tile, { ItemTypes } from './Tile';

const HandContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  border: 1px solid black;
  padding: 10px;
  gap: 10px; /* Add spacing between vertical tiles */
  min-height: 100px; /* Ensure the area is tall enough for vertical tiles */
`;

const HandLabel = styled.div`
  font-weight: bold;
  margin-bottom: 10px;
`;

const PlayerHand = ({ tiles, onDrop }) => {
  const [, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item) => {
      console.log(`Dropped tile ${item.id} on PlayerHand`);
      onDrop(item);
    },
  }));

  return (
    <div>
      <HandLabel>Player's Tile Bank</HandLabel>
      <HandContainer ref={drop}>
        {tiles.map((tile) => (
          <Tile
            key={tile.id}
            side1={tile.side1}
            side2={tile.side2}
            id={tile.id}
            orientation="vertical" // Use vertical images
            reference={HandContainer}
          />
        ))}
      </HandContainer>
    </div>
  );
};

export default PlayerHand;