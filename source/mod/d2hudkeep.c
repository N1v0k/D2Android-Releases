















#define HUD_KEEP_MS 2000



static unsigned hudKept(unsigned deadline, unsigned endedAt, unsigned now, int* kept) {
    if (!deadline || (int) (endedAt - deadline) >= 0) return deadline;
    (*kept)++;
    return now + HUD_KEEP_MS;
}




static unsigned hudKeptTable(int have, unsigned until, unsigned endedAt, unsigned now, int* kept) {
    return have ? hudKept(until, endedAt, now, kept) : until;
}
