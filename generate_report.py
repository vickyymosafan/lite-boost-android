import re
import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
from datetime import datetime

log_path = r"C:\Users\vickymosafan\AppData\Roaming\MetaQuotes\Tester\D0E8209F77C8CF37AD8BF550E51FF075\Agent-127.0.0.1-3000\logs\20260826.log"
with open(log_path, "r", encoding="utf-16le", errors="ignore") as f:
    lines = f.readlines()

start_idx = 0
for i in range(len(lines)-1, -1, -1):
    if "initial deposit 3000.00 USD" in lines[i]:
        start_idx = i
        break

test_lines = lines[start_idx:]

deal_pat = re.compile(r"(\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}:\d{2})\s+deal #(\d+)\s+(buy|sell)\s+([\d\.]+)\s+XAUUSD_REAL\s+at\s+([\d\.]+)\s+done\s+\(based on order #(\d+)\)")

deals = []
for l in test_lines:
    m = deal_pat.search(l)
    if m:
        deals.append({
            "time": datetime.strptime(m.group(1), "%Y.%m.%d %H:%M:%S"),
            "deal_id": int(m.group(2)),
            "type": m.group(3),
            "volume": float(m.group(4)),
            "price": float(m.group(5)),
            "order_id": int(m.group(6)),
        })

positions = []
open_pos = None

for d in deals:
    if open_pos is None:
        open_pos = {
            "open_deal": d["deal_id"],
            "open_time": d["time"],
            "type": d["type"],
            "volume": d["volume"],
            "open_price": d["price"],
            "pos_ticket": d["order_id"]
        }
    else:
        if d["type"] != open_pos["type"]:
            close_price = d["price"]
            open_price = open_pos["open_price"]
            vol = open_pos["volume"]
            if open_pos["type"] == "buy":
                profit = (close_price - open_price) * 100.0 * vol
            else:
                profit = (open_price - close_price) * 100.0 * vol
            
            duration_sec = (d["time"] - open_pos["open_time"]).total_seconds()
            
            p = {
                "ticket": open_pos["pos_ticket"],
                "type": open_pos["type"].upper(),
                "volume": vol,
                "open_time": open_pos["open_time"],
                "open_price": open_price,
                "close_time": d["time"],
                "close_price": close_price,
                "profit": round(profit, 2),
                "duration_sec": duration_sec,
                "deal_in": open_pos["open_deal"],
                "deal_out": d["deal_id"]
            }
            positions.append(p)
            open_pos = None

df = pd.DataFrame(positions)

# Calculate balance curve
initial_balance = 3000.00
df["cumulative_profit"] = df["profit"].cumsum()
df["balance"] = initial_balance + df["cumulative_profit"]
df["peak_balance"] = df["balance"].cummax()
df["drawdown_usd"] = df["peak_balance"] - df["balance"]
df["drawdown_pct"] = (df["drawdown_usd"] / df["peak_balance"]) * 100.0

total_trades = len(df)
wins = df[df["profit"] > 0]
losses = df[df["profit"] < 0]
breakevens = df[df["profit"] == 0]

win_count = len(wins)
loss_count = len(losses)
be_count = len(breakevens)
win_rate = (win_count / total_trades) * 100.0

gross_profit = wins["profit"].sum()
gross_loss = abs(losses["profit"].sum())
net_profit = df["profit"].sum()
profit_factor = gross_profit / gross_loss if gross_loss > 0 else 0.0

max_dd_usd = df["drawdown_usd"].max()
max_dd_pct = df["drawdown_pct"].max()

avg_trade = df["profit"].mean()
avg_win = wins["profit"].mean() if len(wins) > 0 else 0.0
avg_loss = losses["profit"].mean() if len(losses) > 0 else 0.0
largest_win = wins["profit"].max() if len(wins) > 0 else 0.0
largest_loss = losses["profit"].min() if len(losses) > 0 else 0.0

# Consecutive wins/losses
streak = 0
max_cons_wins = 0
max_cons_losses = 0
cur_win_streak = 0
cur_loss_streak = 0

for p in df["profit"]:
    if p > 0:
        cur_win_streak += 1
        cur_loss_streak = 0
        if cur_win_streak > max_cons_wins:
            max_cons_wins = cur_win_streak
    elif p < 0:
        cur_loss_streak += 1
        cur_win_streak = 0
        if cur_loss_streak > max_cons_losses:
            max_cons_losses = cur_loss_streak
    else:
        cur_win_streak = 0
        cur_loss_streak = 0

# Buy vs Sell breakdown
buys = df[df["type"] == "BUY"]
sells = df[df["type"] == "SELL"]
buy_winrate = (len(buys[buys["profit"] > 0]) / len(buys) * 100.0) if len(buys) > 0 else 0.0
sell_winrate = (len(sells[sells["profit"] > 0]) / len(sells) * 100.0) if len(sells) > 0 else 0.0

# Daily breakdown
df["date"] = df["close_time"].dt.date
daily = df.groupby("date").agg(
    trades=("profit", "count"),
    daily_profit=("profit", "sum"),
    wins=("profit", lambda x: (x > 0).sum()),
    losses=("profit", lambda x: (x < 0).sum())
).reset_index()

print("=====================================================")
print("          SCALPZONE EA REAL TICK BACKTEST SUMMARY    ")
print("=====================================================")
print(f"Model: Every tick based on real ticks (Model 4)")
print(f"Latency: 0 ms (Zero Latency)")
print(f"Symbol: XAUUSD_REAL (Custom symbol cloned from XAUUSD.vx)")
print(f"Imported Tick Database: 2,285,563 real ticks (2026.08.05 - 2026.08.12)")
print(f"Processed Ticks in Test: 1,954,784 ticks (6,874 M1 bars)")
print(f"Test Period: 2026.08.05 01:00 - 2026.08.12 23:54")
print(f"Initial Deposit: ${initial_balance:,.2f}")
print(f"Final Balance: ${initial_balance + net_profit:,.2f}")
print(f"Net Profit: ${net_profit:,.2f} ({(net_profit/initial_balance)*100:.2f}%)")
print(f"Gross Profit: ${gross_profit:,.2f}")
print(f"Gross Loss: -${gross_loss:,.2f}")
print(f"Profit Factor: {profit_factor:.2f}")
print(f"Total Trades: {total_trades:,}")
print(f"Winning Trades: {win_count:,} ({win_rate:.2f}%)")
print(f"Losing Trades: {loss_count:,} ({((loss_count/total_trades)*100):.2f}%)")
print(f"Breakeven Trades: {be_count:,} ({((be_count/total_trades)*100):.2f}%)")
print(f"Max Drawdown ($): ${max_dd_usd:,.2f}")
print(f"Max Drawdown (%): {max_dd_pct:.2f}%")
print(f"Average Trade: ${avg_trade:,.2f}")
print(f"Average Win: ${avg_win:,.2f} | Average Loss: ${avg_loss:,.2f}")
print(f"Largest Win: ${largest_win:,.2f} | Largest Loss: ${largest_loss:,.2f}")
print(f"Max Consecutive Wins: {max_cons_wins} | Max Consecutive Losses: {max_cons_losses}")
print(f"Buy Trades: {len(buys)} (Win Rate: {buy_winrate:.2f}%)")
print(f"Sell Trades: {len(sells)} (Win Rate: {sell_winrate:.2f}%)")
print("=====================================================")

# Generate High-Resolution Performance Dashboard Chart
plt.style.use("dark_background")
fig = plt.figure(figsize=(18, 12), dpi=150)
fig.patch.set_facecolor("#0d1117")

# Create grid
gs = fig.add_gridspec(3, 2, height_ratios=[1.2, 0.9, 0.9], hspace=0.35, wspace=0.22)

# Color palette
ACCENT_GREEN = "#238636"
ACCENT_CYAN = "#58a6ff"
ACCENT_RED = "#da3633"
ACCENT_GOLD = "#f1e05a"
TEXT_MUTED = "#8b949e"
CARD_BG = "#161b22"
BORDER_COLOR = "#30363d"

# 1. Equity & Balance Curve (Top Spanning or Left)
ax1 = fig.add_subplot(gs[0, :])
ax1.set_facecolor("#161b22")
for spine in ax1.spines.values():
    spine.set_color(BORDER_COLOR)

ax1.plot(df["close_time"], df["balance"], color=ACCENT_CYAN, linewidth=2, label="Account Balance ($)")
ax1.plot(df["close_time"], df["peak_balance"], color=ACCENT_GOLD, linestyle="--", linewidth=1.2, alpha=0.7, label="Peak High-Water Mark ($)")
ax1.fill_between(df["close_time"], df["balance"], df["peak_balance"], color=ACCENT_RED, alpha=0.15, label="Drawdown Region")
ax1.axhline(initial_balance, color=TEXT_MUTED, linestyle=":", alpha=0.6, label=f"Initial Deposit (${initial_balance:,.0f})")

ax1.set_title("ScalpZone EA — Cumulative Balance & Drawdown Curve (Real Ticks Model 4, Zero Latency)", fontsize=14, fontweight="bold", pad=12, color="#ffffff")
ax1.set_ylabel("Account Balance (USD)", fontsize=11, color="#c9d1d9")
ax1.grid(True, linestyle="--", alpha=0.2, color="#8b949e")
ax1.legend(loc="lower left", framealpha=0.8, facecolor="#0d1117", edgecolor=BORDER_COLOR)
ax1.xaxis.set_major_formatter(mdates.DateFormatter("%b %d\n%H:%M"))

# 2. Underwater Drawdown Curve (Middle Left)
ax2 = fig.add_subplot(gs[1, 0])
ax2.set_facecolor("#161b22")
for spine in ax2.spines.values():
    spine.set_color(BORDER_COLOR)

ax2.fill_between(df["close_time"], -df["drawdown_pct"], 0, color=ACCENT_RED, alpha=0.4)
ax2.plot(df["close_time"], -df["drawdown_pct"], color="#f85149", linewidth=1.5)
ax2.set_title("Underwater Drawdown (%)", fontsize=12, fontweight="bold", pad=10, color="#ffffff")
ax2.set_ylabel("Drawdown %", fontsize=10, color="#c9d1d9")
ax2.grid(True, linestyle="--", alpha=0.2, color="#8b949e")
ax2.xaxis.set_major_formatter(mdates.DateFormatter("%b %d"))

# 3. Daily PnL Bar Chart (Middle Right)
ax3 = fig.add_subplot(gs[1, 1])
ax3.set_facecolor("#161b22")
for spine in ax3.spines.values():
    spine.set_color(BORDER_COLOR)

bar_colors = [ACCENT_GREEN if p >= 0 else ACCENT_RED for p in daily["daily_profit"]]
bars = ax3.bar([str(d)[5:] for d in daily["date"]], daily["daily_profit"], color=bar_colors, width=0.6, edgecolor=BORDER_COLOR)
ax3.axhline(0, color=TEXT_MUTED, linestyle="-", linewidth=0.8)
ax3.set_title("Daily Net Profit / Loss (USD)", fontsize=12, fontweight="bold", pad=10, color="#ffffff")
ax3.set_ylabel("Daily PnL (USD)", fontsize=10, color="#c9d1d9")
ax3.grid(True, linestyle="--", alpha=0.2, color="#8b949e")

for bar, p in zip(bars, daily["daily_profit"]):
    yval = bar.get_height()
    va = "bottom" if yval >= 0 else "top"
    ax3.text(bar.get_x() + bar.get_width()/2.0, yval, f"${p:,.0f}", ha="center", va=va, fontsize=8, color="#c9d1d9", fontweight="bold")

# 4. PnL Distribution (Bottom Left)
ax4 = fig.add_subplot(gs[2, 0])
ax4.set_facecolor("#161b22")
for spine in ax4.spines.values():
    spine.set_color(BORDER_COLOR)

bins = np.linspace(df["profit"].min(), df["profit"].max(), 50)
ax4.hist(df["profit"], bins=bins, color=ACCENT_CYAN, alpha=0.7, edgecolor=BORDER_COLOR)
ax4.axvline(0, color=TEXT_MUTED, linestyle="--", linewidth=1)
ax4.axvline(avg_win, color=ACCENT_GREEN, linestyle=":", linewidth=1.5, label=f"Avg Win: ${avg_win:.2f}")
ax4.axvline(avg_loss, color=ACCENT_RED, linestyle=":", linewidth=1.5, label=f"Avg Loss: ${avg_loss:.2f}")
ax4.set_title("Trade Profit / Loss Distribution", fontsize=12, fontweight="bold", pad=10, color="#ffffff")
ax4.set_xlabel("Profit per Trade (USD)", fontsize=10, color="#c9d1d9")
ax4.set_ylabel("Trade Count", fontsize=10, color="#c9d1d9")
ax4.grid(True, linestyle="--", alpha=0.2, color="#8b949e")
ax4.legend(loc="upper right", framealpha=0.8, facecolor="#0d1117", edgecolor=BORDER_COLOR)

# 5. Key Performance Metrics Table / Summary Card (Bottom Right)
ax5 = fig.add_subplot(gs[2, 1])
ax5.set_facecolor("#161b22")
for spine in ax5.spines.values():
    spine.set_color(BORDER_COLOR)
ax5.axis("off")

table_data = [
    ["Initial Balance", f"${initial_balance:,.2f}", "Total Trades", f"{total_trades:,}"],
    ["Final Balance", f"${initial_balance + net_profit:,.2f}", "Win Rate", f"{win_rate:.2f}% ({win_count} wins)"],
    ["Net Profit", f"${net_profit:,.2f} ({(net_profit/initial_balance)*100:.1f}%)", "Loss Rate", f"{(loss_count/total_trades)*100:.2f}% ({loss_count} loss)"],
    ["Gross Profit", f"${gross_profit:,.2f}", "Breakeven Trades", f"{be_count} ({be_count/total_trades*100:.1f}%)"],
    ["Gross Loss", f"-${gross_loss:,.2f}", "Max Consecutive Loss", f"{max_cons_losses} trades"],
    ["Profit Factor", f"{profit_factor:.2f}", "Max Drawdown ($)", f"${max_dd_usd:,.2f} ({max_dd_pct:.2f}%)"],
    ["Avg Win / Loss", f"${avg_win:.2f} / ${avg_loss:.2f}", "Avg Trade Duration", f"{df['duration_sec'].mean():.0f} sec ({df['duration_sec'].mean()/60:.1f} min)"],
    ["Execution Mode", "0 ms Zero Latency", "Model", "Model 4 (Real Ticks)"]
]

table = ax5.table(
    cellText=table_data,
    colLabels=["Metric", "Value", "Metric", "Value"],
    cellLoc="center",
    loc="center",
    bbox=[0.02, 0.05, 0.96, 0.90]
)
table.auto_set_font_size(False)
table.set_fontsize(8.5)

for (row, col), cell in table.get_celld().items():
    cell.set_edgecolor(BORDER_COLOR)
    if row == 0:
        cell.set_facecolor("#21262d")
        cell.set_text_props(weight="bold", color="#58a6ff")
    else:
        cell.set_facecolor("#161b22" if row % 2 == 0 else "#1c2128")
        if col in [1, 3]:
            text = cell.get_text().get_text()
            if "$" in text and "-" in text:
                cell.set_text_props(color="#f85149", weight="bold")
            elif "$" in text and "+" in text or "win" in text.lower():
                cell.set_text_props(color="#3fb950", weight="bold")
            else:
                cell.set_text_props(color="#c9d1d9")
        else:
            cell.set_text_props(color="#8b949e", weight="bold")

# Save chart to parent agent artifact path and local path
parent_out = r"C:\Users\vickymosafan\.gemini\antigravity\brain\d6aa41cd-2e3e-4134-bc73-1929b58e953b\scalpzone_xauusd_realticks_chart.png"
local_out = r"C:\Users\vickymosafan\.gemini\antigravity\brain\dc260369-d67c-4dfa-9e6a-cb91002401c8\scalpzone_xauusd_realticks_chart.png"

plt.savefig(parent_out, dpi=150, bbox_inches="tight", facecolor=fig.get_facecolor())
plt.savefig(local_out, dpi=150, bbox_inches="tight", facecolor=fig.get_facecolor())
print(f"Chart successfully saved to:")
print(f"1. {parent_out}")
print(f"2. {local_out}")
