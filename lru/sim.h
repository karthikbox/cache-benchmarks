#include "stdio.h"
#include "string.h"
#include "stdlib.h"

#define TRUE 1
#define FALSE 0

#define LRU 0
#define OPT 1
#define LGPS 2

/* used to mark comparison of recency and Smax */
#define S_STACK_IN 1
#define S_STACK_OUT 0


typedef struct pf_struct {
  unsigned long ref_times;
  unsigned long pf_times; 


  unsigned long fwd_distance;
  unsigned long  page_num;
  int isResident; 
  int isHG_page;

  struct pf_struct * LRU_next;
  struct pf_struct * LRU_prev;

  struct pf_struct * LG_next;
  struct pf_struct * HG_next;

  struct pf_struct * OPT_next;
  struct pf_struct * OPT_prev;

  struct pf_struct * LGPS_LRU_next;
  struct pf_struct * LGPS_LRU_prev;

  struct pf_struct * HG_rsd_next;
  struct pf_struct * HG_rsd_prev;
       
   unsigned long    recency;
} page_struct;

page_struct * page_tbl;

unsigned long total_ref_pg;
unsigned long no_dup_refs; /* counter excluding duplicate refs */
unsigned long num_pg_flt;

long free_mem_size, mem_size, vm_size, ref_trc_len;

struct pf_struct * LRU_list_head;
struct pf_struct * LRU_list_tail;

struct pf_struct * HG_list_head;
struct pf_struct * HG_list_tail;

struct pf_struct * OPT_list_head;
struct pf_struct * OPT_list_tail;

struct pf_struct * LG_LRU_page_ptr;

unsigned long HG_page_portion_limit, HG_page_activate_limit;
float HG_PAGE_MEM_PARTION, HG_PAGE_ACTIVATE_LINE;

extern page_struct *find_last_LG_LRU();
extern void add_HG_list_head(page_struct * new_rsd_HG_ptr);
extern void add_LRU_list_head(page_struct *new_ref_ptr);
extern FILE *openReadFile();
extern void insert_LRU_list(page_struct *old_ref_ptr, page_struct *new_ref_ptr);

/* test the variance of LIRS algo */
unsigned long HG_LG_switch;

/* get the range of accessed blocks [1:N] and the number of references */ 
int get_range(FILE *trc_fp, long *p_vm_size, long *p_trc_len)
{
  long ref_blk;
  long count = 0;
  long min, max;

  fseek(trc_fp, 0, SEEK_SET);

  fscanf(trc_fp, "%ld", &ref_blk);
  max = min = ref_blk;

  while (!feof(trc_fp)){
    if (ref_blk < 0)
      return FALSE;
    count++;
    if (ref_blk > max)
      max = ref_blk;
    if (ref_blk < min)
      min = ref_blk;
    fscanf(trc_fp, "%ld", &ref_blk);
  }
  
  printf(" [%d  %d] for %lu refs in the trace\n", min, max, count);
  fseek(trc_fp, 0, SEEK_SET);
  *p_vm_size = max;
  *p_trc_len = count;
  return TRUE;
}


FILE *openReadFile(char file_name[])
{
  FILE *fp;

  fp = fopen(file_name, "r");

  if (!fp) {
    printf("can not find file %s.\n", file_name);
    exit;
    //return NULL;
  }
  
  return fp;
}
