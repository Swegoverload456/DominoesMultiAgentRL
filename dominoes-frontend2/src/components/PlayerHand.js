import React, { useState, useRef, useEffect } from 'react';
import { useDrop } from 'react-dnd';
import styled from 'styled-components';
import Tile, { ItemTypes } from './Tile';

const HandContainer = styled.div`
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  z-index: 2;
`;

const HandArea = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  border: 1px dashed black;
  border-radius: 30px;
  padding: ${({ $hasTiles }) => $hasTiles ? '15px' : '60px'}; // 80px/2 + 20px = 60px
  background-color: rgb(239, 218, 172);
  transform: ${({ expanded }) => expanded ? 'translateY(0)' : 'translateY(100%)'};
  transition: transform ${({speed}) => speed}s ease-in-out;
`;

const ExpandButton = styled.button`
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 0px;
  padding: 5px 10px;
  transition: bottom ${({speed}) => speed}s ease-in-out;
  z-index: 10;
`;

const PlayerHand = ({ tiles, onDrop, speed }) => {
  const [expanded, setExpanded] = useState(false);
  const [handHeight, setHandHeight] = useState(0);
  const handRef = useRef(null);

  const [{ isOver }, drop] = useDrop(() => ({
    accept: ItemTypes.TILE,
    drop: (item) => onDrop(item),
    collect: (monitor) => ({
      isOver: !!monitor.isOver(),
    }),
  }));

  useEffect(() => {
    if(handRef.current){
        setHandHeight(handRef.current.clientHeight)
        console.log("Hand HEIGHT: " + handHeight)
    }
  }, [expanded, tiles]);

  const handleOnClick = () => {
    setExpanded(!expanded);
  };

  const safeTiles = Array.isArray(tiles) ? tiles : [];
  const visibleTiles = expanded ? safeTiles : safeTiles.slice(0, safeTiles.length);

  const hasTiles = safeTiles.length > 0;

  return (
    <HandContainer ref={drop}>
      <ExpandButton speed = {speed} style={{bottom: expanded ? `${handHeight}px` : '0px'}} onClick={handleOnClick}>
        {expanded ? 'Close Hand' : 'Open Hand'}
      </ExpandButton>
      <HandArea ref={(node) =>{
            handRef.current = node;
            drop(node);
        }} 
        expanded={expanded}
        $hasTiles={hasTiles} // Transient prop
        speed = {speed}
        style={{ 
          backgroundColor: isOver ? 'lightgreen' : 'rgb(239, 218, 172)',
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
      </HandArea>
    </HandContainer>
  );
};

export default PlayerHand;