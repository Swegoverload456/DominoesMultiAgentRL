import React, { useState } from 'react';
import { useDrop } from 'react-dnd';
import styled from 'styled-components';
import Tile, { ItemTypes } from './Tile';

const BankArea = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  border: 1px dashed black;
  padding: 10px;
  background-color: #e0e0e0;
`;

const ExpandButton = styled.button`
  align-items: center;
  margin-top: 10px;
`;

const Bank = ({ tiles, onDrop }) => {
  const [expanded, setExpanded] = useState(false);

  const [, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item) => onDrop(item),
  }));

  // Ensure tiles is an array; if not, default to an empty array
  const safeTiles = Array.isArray(tiles) ? tiles : [];
  const visibleTiles = expanded ? safeTiles : safeTiles.slice(0, 3);

  return (
    <div>
      <BankArea ref={drop}>
        {visibleTiles.map((tile, index) => (
          <Tile
            key={tile.id}
            side1={tile.side1}
            side2={tile.side2}
            id={tile.id}
            orientation="vertical" // Use vertical images
            reference={BankArea}
          />
        ))}
      </BankArea>
      <ExpandButton onClick={() => setExpanded(!expanded)}>
        {expanded ? 'Collapse' : 'Expand'}
      </ExpandButton>
    </div>
  );
};

export default Bank;