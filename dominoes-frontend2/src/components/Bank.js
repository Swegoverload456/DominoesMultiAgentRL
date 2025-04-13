import React, { useState, useRef, useEffect } from 'react';
import { useDrop } from 'react-dnd';
import styled from 'styled-components';
import Tile, { ItemTypes } from './Tile';

const BankContainer = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 2;
`;

const BankArea = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  border: 1px dashed black;
  border-radius: 30px;
  padding: 20px;
  background-color: rgb(172, 218, 239);
  transform: ${({ expanded }) => expanded ? 'translateY(0)' : 'translateY(-100%)'};
  transition: transform ${({speed}) => speed}s ease-in-out;
`;

const ExpandButton = styled.button`
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  padding: 5px 10px;
  transition: top ${({speed}) => speed}s ease-in-out;
  z-index: 10;
`;

const Bank = ({ tiles, onDrop, speed }) => {
  const [expanded, setExpanded] = useState(false);
  const [bankHeight, setBankHeight] = useState(0);
  const bankRef = useRef(null);

  const [{ isOver }, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item) => onDrop(item),
    collect: (monitor) => ({
      isOver: !!monitor.isOver(),
    }),
  }));

  useEffect(() => {
    if (bankRef.current) {
      setBankHeight(bankRef.current.clientHeight);
      console.log("BANK HEIGHT: " + bankHeight)
    }
  }, [expanded, tiles]); // Recalculate when expanded state or tiles change

  const handleOnClick = () => {
    setExpanded(!expanded);
  };

  const safeTiles = Array.isArray(tiles) ? tiles : [];
  const visibleTiles = expanded ? safeTiles : safeTiles.slice(0, safeTiles.length);

  return (
    <div>
      <BankContainer>
        <BankArea 
          ref={(node) => {
            bankRef.current = node;
            drop(node);
          }}
          expanded={expanded}
          speed = {speed}
          style={{ 
            backgroundColor: isOver ? 'lightgreen' : 'rgb(172, 218, 239)'
          }}
        >
          {visibleTiles.map((tile) => (
            <Tile
              key={tile.id}
              side1={tile.side1}
              side2={tile.side2}
              id={tile.id}
              dir="vertical"
              onDragStart={onDragStart} // Add this prop
            />
          ))}
        </BankArea>
        <ExpandButton 
          speed = {speed}
          style={{ 
            top: expanded ? `${bankHeight}px` : '0px',
          }} 
          onClick={handleOnClick}
        >
          {expanded ? 'Close Bank' : 'Open Bank'}
        </ExpandButton>
      </BankContainer>
    </div>
  );
};

export default Bank;