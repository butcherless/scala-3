package com.cmartin.learn.http

object HtmlPage:

  private val endpoint = "/api/sales/stream"
  val index            = s"""
    <!DOCTYPE html>
    <html>
    <head>
      <title>Sales Dashboard</title>
      <style>
        body {
          font-family: Arial, sans-serif;
          max-width: 800px;
          margin: 50px auto;
          padding: 20px;
          background: #f5f5f5;
        }
        .dashboard {
          background: white;
          padding: 30px;
          border-radius: 8px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        h1 { color: #333; }
        .metric {
          font-size: 48px;
          color: #2ecc71;
          margin: 20px 0;
        }
        .label {
          font-size: 18px;
          color: #666;
          margin-bottom: 10px;
        }
        .status {
          padding: 10px;
          border-radius: 4px;
          margin-top: 20px;
        }
        .connected { background: #d4edda; color: #155724; }
        .disconnected { background: #f8d7da; color: #721c24; }
      </style>
    </head>
    <body>
      <div class="dashboard">
        <h1>Real-Time Sales Dashboard</h1>
        <div class="label">Total Sales</div>
        <div class="metric" id="totalSales">$$0.00</div>
        <div class="label">Events Processed: <span id="eventCount">0</span></div>
        <div id="status" class="status disconnected">Connecting...</div>
      </div>

      <script>
        const eventSource = new EventSource('${endpoint}');
        const statusEl = document.getElementById('status');
        const totalSalesEl = document.getElementById('totalSales');
        const eventCountEl = document.getElementById('eventCount');

        eventSource.addEventListener('sales-update', (e) => {
          const data = JSON.parse(e.data);
          totalSalesEl.textContent = '$$' + data.totalSales.toFixed(2);
          eventCountEl.textContent = data.eventCount;
        });

        eventSource.onopen = () => {
          statusEl.textContent = 'Connected';
          statusEl.className = 'status connected';
        };

        eventSource.onerror = () => {
          statusEl.textContent = 'Disconnected';
          statusEl.className = 'status disconnected';
        };
      </script>
    </body>
    </html>
  """
