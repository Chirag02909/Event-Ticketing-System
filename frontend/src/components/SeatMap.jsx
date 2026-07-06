import React from 'react';

/**
 * Interactive Seat Map component.
 * 
 * @param {Array} seats - List of SeatResponseDTO objects.
 * @param {Array} selectedSeatIds - List of currently selected seat IDs.
 * @param {Function} onSeatSelect - Callback when a seat is clicked.
 * @param {number} maxSelection - Maximum seats that can be held at once.
 * @param {Array} justUpdatedSeatIds - List of seat IDs recently updated via WebSocket.
 */
export const SeatMap = ({
  seats = [],
  selectedSeatIds = [],
  onSeatSelect,
  maxSelection = 10,
  justUpdatedSeatIds = [],
}) => {
  // Group seats by rowLabel
  const rows = seats.reduce((acc, seat) => {
    const row = seat.rowLabel;
    if (!acc[row]) {
      acc[row] = [];
    }
    acc[row].push(seat);
    return acc;
  }, {});

  // Sort rows alphabetically
  const sortedRowKeys = Object.keys(rows).sort();
  
  // Sort seats in each row by seatNumber numerically
  sortedRowKeys.forEach((row) => {
    rows[row].sort((a, b) => 
      a.seatNumber.localeCompare(b.seatNumber, undefined, { numeric: true, sensitivity: 'base' })
    );
  });

  const handleSeatClick = (seat) => {
    if (seat.status !== 'AVAILABLE') return;
    
    const isSelected = selectedSeatIds.includes(seat.id);
    if (!isSelected && selectedSeatIds.length >= maxSelection) {
      alert(`You can select a maximum of ${maxSelection} seats.`);
      return;
    }
    
    if (onSeatSelect) {
      onSeatSelect(seat);
    }
  };

  // Helper to determine category styling
  const getCategoryBorder = (category) => {
    switch (category?.toUpperCase()) {
      case 'VIP':
        return '3px solid hsl(45, 100%, 50%)'; // Gold border
      case 'PREMIUM':
        return '1.5px solid var(--accent)'; // Indigo border
      default:
        return '1.5px solid rgba(255, 255, 255, 0.2)'; // Generic border
    }
  };

  return (
    <div className="seat-map-wrapper">
      {/* Stage Visual */}
      <div className="stage-view">Stage</div>

      {/* Seat Grid */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px', width: '100%' }}>
        {sortedRowKeys.length === 0 ? (
          <div style={{ color: 'var(--text-muted)', padding: '20px' }}>No layout generated yet.</div>
        ) : (
          sortedRowKeys.map((rowLabel) => (
            <div key={rowLabel} className="seat-row">
              {/* Left Row Label */}
              <div className="row-label">{rowLabel}</div>
              
              {/* Seats */}
              <div className="seat-grid-row">
                {rows[rowLabel].map((seat) => {
                  const isSelected = selectedSeatIds.includes(seat.id);
                  const isJustUpdated = justUpdatedSeatIds.includes(seat.id);
                  const statusClass = seat.status.toLowerCase(); // available, held, booked
                  
                  return (
                    <div
                      key={seat.id}
                      className={`seat ${statusClass} ${isSelected ? 'selected' : ''} ${isJustUpdated ? 'just-updated' : ''}`}
                      style={{
                        border: isSelected ? 'none' : getCategoryBorder(seat.category),
                      }}
                      onClick={() => handleSeatClick(seat)}
                      title={`Seat: ${seat.seatNumber} | Category: ${seat.category} | Price: ₹${seat.price}`}
                    >
                      {/* Short seat number inside circle, e.g. "A05" -> "5" or "VIP-3" -> "3" */}
                      {seat.seatNumber.replace(/^[a-zA-Z]+-?0*/, '')}
                    </div>
                  );
                })}
              </div>
              
              {/* Right Row Label */}
              <div className="row-label" style={{ marginLeft: '12px', marginRight: 0, textAlign: 'left' }}>
                {rowLabel}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Legends and Info */}
      <div style={{ width: '100%', borderTop: '1px solid var(--border-main)', paddingTop: '20px' }}>
        <div className="seat-legend">
          <div className="legend-item">
            <div className="legend-color available"></div>
            <span>Available</span>
          </div>
          <div className="legend-item">
            <div className="legend-color held"></div>
            <span>Held (Other User)</span>
          </div>
          <div className="legend-item">
            <div className="legend-color booked"></div>
            <span>Booked</span>
          </div>
          <div className="legend-item">
            <div className="legend-color selected"></div>
            <span>Selected (You)</span>
          </div>
        </div>

        {/* Pricing Zone Tiers */}
        <div style={{
          display: 'flex',
          justifyContent: 'center',
          gap: '24px',
          marginTop: '16px',
          fontSize: '0.85rem',
          flexWrap: 'wrap'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: '12px', height: '12px', border: '3px solid hsl(45, 100%, 50%)', borderRadius: '3px' }}></div>
            <span style={{ fontWeight: 600 }}>VIP Zone</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: '12px', height: '12px', border: '1.5px solid var(--accent)', borderRadius: '3px' }}></div>
            <span style={{ fontWeight: 600 }}>Premium Zone</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <div style={{ width: '12px', height: '12px', border: '1.5px solid rgba(255, 255, 255, 0.2)', borderRadius: '3px' }}></div>
            <span style={{ fontWeight: 600 }}>General Zone</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SeatMap;
