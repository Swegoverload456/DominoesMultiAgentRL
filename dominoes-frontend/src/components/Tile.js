import React, { useRef, useEffect, memo } from 'react';
import { useDrag } from 'react-dnd';
import styled from 'styled-components';

const TileContainer = styled.div`
  display: flex;
  border: 0px solid black;
  margin: 5px;
  padding: 0px;
  background-color: white;
  cursor: move;
  border-radius: 8px;
  transform: ${({ flipped }) => (flipped ? 'scaleX(-1)' : 'scaleX(1)')};
`;

const DominoImage = styled.img`
  width: ${({ orientation }) => (orientation === 'vertical' ? '40px' : '80px')};
  height: ${({ orientation }) => (orientation === 'vertical' ? '80px' : '40px')};
  object-fit: contain;
`;

const ItemTypes = {
  TILE: 'tile',
};

const Tile = ({ side1, side2, id, flipped = false, orientation = 'horizontal', boardRect, dropPosition, finalPosition }) => {
  const [{ isDragging, clientOffset }, drag] = useDrag(() => ({
    type: ItemTypes.TILE,
    item: { side1, side2, id },
    collect: (monitor) => ({
      isDragging: !!monitor.isDragging(),
      clientOffset: monitor.getClientOffset(),
    }),
  }));

  let dir = 'horizontal';
  if (side1 === side2) {
    dir = 'vertical';
  }

  const [imageSide1, imageSide2] = side1 <= side2 ? [side1, side2] : [side2, side1];
  const imageFolder = orientation === 'vertical' ? 'vertical' : 'horizontal';
  const imagePath = `/images/dominos/${imageFolder}/${imageSide1}${imageSide2}.png`;
  const shouldFlip = flipped ? !(side1 <= side2) : side1 > side2;

  useEffect(() => {
    // Only log and calculate for tiles that have been dropped
    if (dropPosition || finalPosition) {
      console.log(`Tile.js - Tile ${id} - dropPosition:`, dropPosition);
      console.log(`Tile.js - Tile ${id} - finalPosition:`, finalPosition);
      console.log(`Tile.js - Tile ${id} - boardRect:`, boardRect);

      if (!finalPosition || !boardRect) {
        console.log(`Tile.js - Tile ${id} - Missing finalPosition or boardRect, skipping calculation`);
        return;
      }

      const tileX = finalPosition.x;
      const tileY = finalPosition.y;
      console.log(`Tile.js - Tile ${id} - Final position: (${tileX}, ${tileY})`);

      const isInsideBoard =
        tileX >= boardRect.left &&
        tileX <= boardRect.right &&
        tileY >= boardRect.top &&
        tileY <= boardRect.bottom;
      console.log(`Tile.js - Tile ${id} - Is inside board: ${isInsideBoard}`);

      if (isInsideBoard) {
        const distanceToLeft = Math.abs(tileX - boardRect.left);
        const distanceToRight = Math.abs(tileX - boardRect.right);
        const distanceToTop = Math.abs(tileY - boardRect.top);
        const distanceToBottom = Math.abs(tileY - boardRect.bottom);

        const distances = [
          { wall: 'left', distance: distanceToLeft },
          { wall: 'right', distance: distanceToRight },
          { wall: 'top', distance: distanceToTop },
          { wall: 'bottom', distance: distanceToBottom },
        ];

        const closest = distances.reduce((min, current) =>
          current.distance < min.distance ? current : min,
          distances[0]
        );

        console.log(
          `Tile.js - Tile ${id} - Closest wall: ${closest.wall}, Distance: ${closest.distance}px, Position: (${tileX}, ${tileY})`
        );

        if (closest.wall === 'left' || closest.wall === 'right') {
          console.log(`Tile.js - Tile ${id} - Consider flipping to vertical orientation`);
        } else if (closest.wall === 'top' || closest.wall === 'bottom') {
          console.log(`Tile.js - Tile ${id} - Consider flipping to horizontal orientation`);
        }
      } else {
        console.log(`Tile.js - Tile ${id} - Not inside board area`);
      }
    }
  }, [finalPosition, boardRect, id, dropPosition]);

  return (
    <TileContainer ref={drag} flipped={shouldFlip} style={{ opacity: isDragging ? 0.5 : 1 }}>
      <DominoImage src={imagePath} orientation={orientation} />
    </TileContainer>
  );
};

export default memo(Tile);
export { ItemTypes };