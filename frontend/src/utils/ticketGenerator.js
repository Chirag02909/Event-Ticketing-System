/**
 * Generates a self-contained, beautifully styled HTML ticket and triggers its download.
 * @param {Object} booking - The booking details object.
 */
export const downloadTicket = (booking) => {
  const {
    bookingRef = 'TKT-PENDING',
    eventTitle = 'Unknown Event',
    venueName = 'Unknown Venue',
    eventDate = '',
    seats = [],
    ticketNumbers = [],
    totalAmount = 0
  } = booking;

  const formattedDate = eventDate
    ? new Date(eventDate).toLocaleDateString('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    : 'Date not specified';

  const seatNames = (seats || []).map((s) => s.seatNumber).join(', ') || 'N/A';
  const ticketsList = (ticketNumbers && ticketNumbers.length > 0) ? ticketNumbers.join(', ') : bookingRef;

  // Standalone premium HTML ticket template
  const ticketHTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ticket - ${eventTitle}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #0f172a;
      --card-bg: rgba(30, 41, 59, 0.7);
      --primary: #8b5cf6;
      --primary-glow: rgba(139, 92, 246, 0.4);
      --accent: #06b6d4;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --border: rgba(255, 255, 255, 0.08);
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      background-color: var(--bg);
      color: var(--text);
      font-family: 'Outfit', sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
      background-image: 
        radial-gradient(circle at 10% 20%, rgba(139, 92, 246, 0.15) 0%, transparent 40%),
        radial-gradient(circle at 90% 80%, rgba(6, 118, 212, 0.15) 0%, transparent 40%);
    }

    .no-print-area {
      margin-bottom: 24px;
    }

    .btn {
      background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
      color: white;
      border: none;
      padding: 12px 28px;
      font-family: 'Outfit', sans-serif;
      font-weight: 600;
      font-size: 1rem;
      border-radius: 50px;
      cursor: pointer;
      box-shadow: 0 4px 20px var(--primary-glow);
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .btn:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 24px rgba(139, 92, 246, 0.6);
    }

    /* Ticket Layout */
    .ticket-container {
      width: 100%;
      max-width: 700px;
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 24px;
      overflow: hidden;
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
      backdrop-filter: blur(12px);
      position: relative;
      display: flex;
      flex-direction: column;
    }

    /* Decorative side cutouts */
    .ticket-container::before,
    .ticket-container::after {
      content: '';
      position: absolute;
      width: 30px;
      height: 30px;
      background: var(--bg);
      border-radius: 50%;
      top: 68%;
      transform: translateY(-50%);
      z-index: 10;
    }
    .ticket-container::before {
      left: -15px;
      box-shadow: inset -5px 0 8px rgba(0,0,0,0.3);
    }
    .ticket-container::after {
      right: -15px;
      box-shadow: inset 5px 0 8px rgba(0,0,0,0.3);
    }

    /* Header Panel */
    .ticket-header {
      padding: 32px;
      background: linear-gradient(to right, rgba(139, 92, 246, 0.1), rgba(6, 182, 212, 0.05));
      border-bottom: 1px dashed var(--border);
      position: relative;
    }

    .ticket-badge {
      display: inline-block;
      padding: 6px 12px;
      background: rgba(139, 92, 246, 0.2);
      border: 1px solid var(--primary);
      color: #c084fc;
      border-radius: 50px;
      font-size: 0.8rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 16px;
    }

    .event-title {
      font-size: 2.2rem;
      font-weight: 800;
      line-height: 1.2;
      background: linear-gradient(to right, #f8fafc, #cbd5e1);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 12px;
    }

    /* Details Grid */
    .ticket-body {
      padding: 32px;
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 24px;
    }

    .info-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .info-label {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: var(--text-muted);
      font-weight: 600;
    }

    .info-value {
      font-size: 1.1rem;
      font-weight: 600;
    }

    .mono-value {
      font-family: 'Space Mono', monospace;
      font-weight: 700;
      color: var(--accent);
    }

    /* Footer Stub Area */
    .ticket-stub {
      padding: 32px;
      background: rgba(15, 23, 42, 0.4);
      border-top: 1px dashed var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 20px;
    }

    /* Simulated Barcode */
    .barcode-area {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
    }

    .barcode {
      display: flex;
      height: 60px;
      align-items: stretch;
      background: white;
      padding: 8px 16px;
      border-radius: 6px;
    }

    .barcode span {
      width: 2px;
      background: #000;
      margin-right: 2px;
    }
    
    .barcode span.thick {
      width: 4px;
    }
    
    .barcode span.medium {
      width: 3px;
    }

    .barcode span.space {
      background: transparent;
      width: 3px;
    }

    .barcode-text {
      font-family: 'Space Mono', monospace;
      font-size: 0.75rem;
      color: var(--text-muted);
      letter-spacing: 2px;
    }

    /* Ticket Stub Info */
    .stub-details {
      text-align: right;
    }

    .stub-ref {
      font-family: 'Space Mono', monospace;
      font-size: 1.2rem;
      font-weight: 700;
      color: var(--text);
    }

    @media (max-width: 600px) {
      .ticket-body {
        grid-template-columns: 1fr;
      }
      .ticket-stub {
        flex-direction: column;
        align-items: center;
        text-align: center;
      }
      .stub-details {
        text-align: center;
      }
    }

    /* Print styling */
    @media print {
      body {
        background: white;
        color: black;
        padding: 0;
      }
      .no-print-area {
        display: none;
      }
      .ticket-container {
        border: 1px solid #ccc;
        box-shadow: none;
        background: white;
        max-width: 100%;
      }
      .ticket-container::before,
      .ticket-container::after {
        display: none;
      }
      .event-title {
        -webkit-text-fill-color: initial;
        color: black;
      }
      .info-value, .mono-value {
        color: black !important;
      }
    }
  </style>
</head>
<body>

  <div class="no-print-area">
    <button class="btn" onclick="window.print()">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="6 9 6 2 18 2 18 9"></polyline>
        <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
        <rect x="6" y="14" width="12" height="8"></rect>
      </svg>
      Print / Save as PDF
    </button>
  </div>

  <div class="ticket-container">
    <div class="ticket-header">
      <span class="ticket-badge">Admission Ticket</span>
      <h1 class="event-title">${eventTitle}</h1>
    </div>

    <div class="ticket-body">
      <div class="info-group">
        <span class="info-label">Date & Time</span>
        <span class="info-value">${formattedDate}</span>
      </div>

      <div class="info-group">
        <span class="info-label">Venue</span>
        <span class="info-value">${venueName}</span>
      </div>

      <div class="info-group">
        <span class="info-label">Seats</span>
        <span class="info-value mono-value">${seatNames}</span>
      </div>

      <div class="info-group">
        <span class="info-label">Booking Reference</span>
        <span class="info-value mono-value">${bookingRef}</span>
      </div>

      <div class="info-group">
        <span class="info-label">Digital Tickets</span>
        <span class="info-value" style="font-size: 0.95rem;">${ticketsList}</span>
      </div>

      <div class="info-group">
        <span class="info-label">Total Amount</span>
        <span class="info-value">₹${totalAmount.toFixed(2)}</span>
      </div>
    </div>

    <div class="ticket-stub">
      <div class="barcode-area">
        <div class="barcode">
          <span class="thick"></span>
          <span class="space"></span>
          <span></span>
          <span class="medium"></span>
          <span class="space"></span>
          <span class="thick"></span>
          <span></span>
          <span class="space"></span>
          <span class="medium"></span>
          <span class="thick"></span>
          <span class="space"></span>
          <span></span>
          <span class="thick"></span>
          <span class="space"></span>
          <span></span>
          <span class="medium"></span>
          <span class="space"></span>
          <span class="thick"></span>
          <span></span>
          <span class="space"></span>
          <span class="medium"></span>
          <span class="thick"></span>
        </div>
        <span class="barcode-text">${bookingRef}</span>
      </div>

      <div class="stub-details">
        <span class="info-label" style="display: block; margin-bottom: 4px;">Gate Pass</span>
        <span class="stub-ref">${bookingRef}</span>
      </div>
    </div>
  </div>

</body>
</html>`;

  // Create Blob and trigger download
  const blob = new Blob([ticketHTML], { type: 'text/html' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `ticket-${bookingRef}.html`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};
