# Teaching Notes

## User Context
Teaching HTTP servers through the lens of the JX OS implementation.

## Known Codebase
- WebServer: accepts connections, can use threads or JX domains for concurrency
- TCPWebWorker: parses HTTP requests, serves files or canned responses, constructs headers
- Uses java.net.Socket, ServerSocket

## Pedagogical Approach
Focus on concrete understanding through the actual implementation. Keep lessons short with hands-on exploration. Build up from TCP → HTTP protocol → request parsing → response building.

## Preferences
Start from fundamentals, go step by step. Tie each lesson to the actual code.
