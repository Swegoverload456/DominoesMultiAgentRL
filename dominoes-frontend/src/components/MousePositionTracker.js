import React, { useState, useRef, useEffect } from 'react';

let [position, setPosition] = [0];

function MousePositionTracker(ref) {
  [position, setPosition] = useState({ x: 0, y: 0 });
  const componentRef = useRef(ref);

  const handleMouseMove = (event) => {
    const rect = componentRef.current.getBoundingClientRect();
    setPosition({
      x: (event.clientX - rect.left - ((rect.right-rect.left)/2)),
      y: event.clientY - rect.top,
    });
  };

  useEffect(() => {
    const component = componentRef.current;
    component.addEventListener('mousemove', handleMouseMove);

    return () => {
      component.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  return (
    <div
      ref={componentRef}
      style={{
        width: ref.width,
        height: '100%',
        border: '1px solid black',
        position: 'relative',
        top: '-100%',
        visibility: 'hidden'
      }}
    >
      Mouse position: x: {position.x}, y: {position.y}
    </div>
  );
}

export default MousePositionTracker;
export {position};