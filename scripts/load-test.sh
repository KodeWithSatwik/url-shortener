#!/bin/bash
# load-test.sh — 100 requests simulating different client IPs via X-Forwarded-For
# Rate limiter is per-IP; this test distributes load across 10 simulated IPs.

BASE="http://localhost:8080"
TOTAL=100
PASS=0
FAIL=0

# Status code tallies (using individual vars for sh compat)
S200=0; S201=0; S302=0; S404=0; S429=0; S410=0; SOTHER=0

echo "========================================"
echo " URL Shortener Load Test"
echo " Target   : $TOTAL requests"
echo " Strategy : 10 simulated IPs via X-Forwarded-For"
echo " Rate     : ~100 req/min (0.6s interval)"
echo " Base URL : $BASE"
echo "========================================"
echo ""

# Pre-create a seed URL to use for GET/analytics requests
echo "Creating seed URL..."
SEED_RESP=$(curl -s -X POST "$BASE/api/v1/urls" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 10.0.0.99" \
  -d '{"originalUrl":"https://load-test.example.com/seed"}')
SEED_CODE=$(echo "$SEED_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('shortCode',''))" 2>/dev/null)

if [ -z "$SEED_CODE" ]; then
  echo "ERROR: Could not create seed URL. Is the app running?"
  echo "Response: $SEED_RESP"
  exit 1
fi
echo "Seed short code: $SEED_CODE"
echo ""
echo "Starting load test..."
echo ""

START_MS=$(python3 -c "import time; print(int(time.time()*1000))")

for i in $(seq 1 $TOTAL); do
  # Rotate across 10 simulated IPs (10 req/IP = well under 20 req/min limit)
  IP_INDEX=$(( (i - 1) % 10 + 1 ))
  CLIENT_IP="10.0.0.$IP_INDEX"

  # Rotate request types
  REQ_TYPE=$(( (i - 1) % 4 ))

  case $REQ_TYPE in
    0)
      # POST shorten — unique URL per request
      STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$BASE/api/v1/urls" \
        -H "Content-Type: application/json" \
        -H "X-Forwarded-For: $CLIENT_IP" \
        -d "{\"originalUrl\":\"https://load-test.example.com/path-$i\"}")
      ;;
    1)
      # GET redirect — follow 302
      STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        --max-redirs 0 \
        -H "X-Forwarded-For: $CLIENT_IP" \
        "$BASE/$SEED_CODE")
      ;;
    2)
      # GET analytics
      STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "X-Forwarded-For: $CLIENT_IP" \
        "$BASE/api/v1/urls/$SEED_CODE/analytics")
      ;;
    3)
      # GET health
      STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        -H "X-Forwarded-For: $CLIENT_IP" \
        "$BASE/actuator/health")
      ;;
  esac

  # Count by status
  case $STATUS in
    200) S200=$((S200+1)) ;;
    201) S201=$((S201+1)) ;;
    302) S302=$((S302+1)) ;;
    404) S404=$((S404+1)) ;;
    410) S410=$((S410+1)) ;;
    429) S429=$((S429+1)) ;;
    *)   SOTHER=$((SOTHER+1)) ;;
  esac

  if [[ "$STATUS" == "2"* || "$STATUS" == "302" ]]; then
    PASS=$((PASS+1))
    printf "  [%3d] IP=%-12s type=%d status=%s ✓\n" "$i" "$CLIENT_IP" "$REQ_TYPE" "$STATUS"
  else
    FAIL=$((FAIL+1))
    printf "  [%3d] IP=%-12s type=%d status=%s ✗\n" "$i" "$CLIENT_IP" "$REQ_TYPE" "$STATUS"
  fi

  sleep 0.6
done

END_MS=$(python3 -c "import time; print(int(time.time()*1000))")
ELAPSED=$((END_MS - START_MS))
RATE=$(python3 -c "print(f'{$TOTAL * 60000 / $ELAPSED:.1f}')")

echo ""
echo "========================================"
echo " Results"
echo "========================================"
printf " Total requests : %d\n" "$TOTAL"
printf " Passed         : %d\n" "$PASS"
printf " Failed         : %d\n" "$FAIL"
printf " Success rate   : %.1f%%\n" "$(python3 -c "print($PASS*100/$TOTAL)")"
printf " Elapsed        : %dms\n" "$ELAPSED"
printf " Actual rate    : %s req/min\n" "$RATE"
echo ""
echo " Status code breakdown:"
[ $S200   -gt 0 ] && echo "   HTTP 200 : $S200  (analytics / health)"
[ $S201   -gt 0 ] && echo "   HTTP 201 : $S201  (URL created)"
[ $S302   -gt 0 ] && echo "   HTTP 302 : $S302  (redirects)"
[ $S404   -gt 0 ] && echo "   HTTP 404 : $S404  (not found)"
[ $S410   -gt 0 ] && echo "   HTTP 410 : $S410  (expired)"
[ $S429   -gt 0 ] && echo "   HTTP 429 : $S429  (rate limited)"
[ $SOTHER -gt 0 ] && echo "   Other    : $SOTHER"
echo "========================================"
echo ""
echo " Rate limit config: 20 req/min per IP"
echo " Simulated IPs    : 10 (10 req each = no client hits the limit)"
