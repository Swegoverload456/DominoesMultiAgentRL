import styled from 'styled-components';
import React, { memo } from 'react';
import { useDrag } from 'react-dnd';

const DominoImage = styled.img`
  width: ${({ orientation }) => (orientation === 'vertical' ? '40px' : '80px')};
  height: ${({ orientation }) => (orientation === 'vertical' ? '80px' : '40px')};
  object-fit: contain;
  margin: 5px;
`;

const ItemTypes = {
  TILE: 'tile',
};

const Tile = ({ side1, side2, id, flipped = false, dir = 'horizontal', onDragStart }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: ItemTypes.TILE,
    item: () => {
      if (onDragStart) {
        return onDragStart({ side1, side2, id });
      }
      return { side1, side2, id };
    },
    end: (item, monitor) => {
      if (!monitor.didDrop()) {
        // Handle cancel if needed
      }
    },
    collect: (monitor) => ({
      isDragging: !!monitor.isDragging(),
    }),
  }));

  const [imageSide1, imageSide2] = flipped ? 
    (side1 <= side2 ? [side2, side1] : [side1, side2]) : 
    (side1 <= side2 ? [side1, side2] : [side2, side1]);

  const imageFolder = dir === 'vertical' ? 'vertical' : 'horizontal';
  const imagePath = `/dominos/${imageFolder}/${imageSide1}${imageSide2}.png`;
  
  return (
    <DominoImage 
      ref={drag}
      src={imagePath} 
      orientation={dir}
      style={{
        opacity: isDragging ? 0.5 : 1,
        cursor: 'move',
      }}
    />
  );
};

export default memo(Tile);
export { ItemTypes };