// Quiz handler for lessons
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('quiz');
  if (!form) return;

  const answers = {
    q1: 'b', // malloc returns virtual memory that may not be physically contiguous or DMA-accessible
    q2: 'b', // Call dma_map_single(dev, buf, 512, DMA_TO_DEVICE) to get the DMA address
    q3: 'b', // Invalidate the cache for that buffer region
    q4: 'b', // The controller indexes it with frame number × 4 bytes; alignment simplifies hardware
  };

  const explanations = {
    q1: 'malloc returns virtual memory from the heap which may be fragmented (not physically contiguous) and may not be in a DMA-accessible region. Use dma_alloc_coherent or equivalent.',
    q2: 'kmalloc returns kernel virtual address. The USB controller needs a bus/DMA address. dma_map_single returns the DMA address and handles cache coherency.',
    q3: 'After DMA_FROM_DEVICE, the controller has written to RAM but CPU cache may have stale data. Invalidate (discard) the cache lines so CPU fetches fresh data from RAM.',
    q4: 'The UHCI frame list is an array of 1024 pointers (4 bytes each = 4KB). The controller uses (frame_number % 1024) * 4 as offset. 4KB alignment ensures frame 0 starts at a 4KB boundary, simplifying hardware.',
  };

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    let score = 0;
    const results = document.getElementById('results');
    let html = '<h3>Results</h3><ul>';

    Object.keys(answers).forEach(q => {
      const selected = form.querySelector(`input[name="${q}"]:checked`);
      const isCorrect = selected && selected.value === answers[q];
      if (isCorrect) score++;
      
      html += `<li style="margin: 0.75rem 0;">
        <strong>${q.toUpperCase()}: </strong>
        <span style="color: ${isCorrect ? '#16a34a' : '#dc2626'}">
          ${isCorrect ? '✓ Correct' : '✗ Incorrect'}
        </span><br>
        <small>${explanations[q]}</small>
      </li>`;
    });

    html += `</ul><p><strong>Score: ${score}/${Object.keys(answers).length}</strong></p>`;
    results.innerHTML = html;
    results.className = score === Object.keys(answers).length ? 'correct' : 'incorrect';
    results.classList.remove('hidden');
    results.scrollIntoView({ behavior: 'smooth' });
  });
});